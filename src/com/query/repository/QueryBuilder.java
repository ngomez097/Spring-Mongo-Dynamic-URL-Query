package com.query.repository;

import com.query.exception.BadRequestException;
import com.query.model.CountModel;
import com.query.model.PageData;
import com.query.util.DynamicTypes;
import com.query.util.Utils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.ParameterizedType;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QueryBuilder<T> {

    private enum FILTER_SYNTAX {
        FILTER_STRING,
        OPERATION,
        FIELD,
        CAST_TYPE
    }

    private enum OPERATIONS {
        EQ,
        NEQ,
        GT,
        GTE,
        LT,
        LTE,
        REGEX,
        IN

    }

    private final MongoTemplate mongoTemplate;
    private final Class<T> dataClass;
    private final String collectionName;
    private final Logger logger;

    public QueryBuilder(MongoTemplate mongoTemplate, Class<T> dataClass, String collectionName) {
        this.mongoTemplate = mongoTemplate;
        this.dataClass = dataClass;
        this.collectionName = collectionName;
        this.logger = Logger.getLogger("QueryBuilder");
    }

    public PageData<T> executeQuery() {
        return executeQuery(QueryBuilderOptions.build());
    }

    public PageData<T> executeQuery(QueryBuilderOptions options) throws BadRequestException {
        PageData<T> result = new PageData<>();

        // Obteniendo la query
        ServletRequestAttributes servletRequestAttributes = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
        String queryString = "";
        if (servletRequestAttributes != null && options.getEnableConf().isAnyEnable()) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            queryString = request.getQueryString() == null ? "" : request.getQueryString();
        }

        // Decodificando la query
        QueryBuilderStr extraQuery = options.getExtraQuery();
        String extraQueryStr = extraQuery == null ? "" : extraQuery.getQuery();

        // Identificar que queries están vaciás
        try {
            queryString = URLDecoder.decode(queryString, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (queryString == null) {
            throw new BadRequestException("invalid to decode URL");
        }

        // Si no hay nada para hacer directamente buscar en la BD
        if (queryString.equals("") && extraQueryStr.equals("")) {
            result.data = mongoTemplate.findAll(dataClass);
            return result;
        }

        // Log de la query
        if (!queryString.equals("")) {
            if(extraQueryStr.equals("")) logger.info(String.format("DB: %s, query: %s\n", collectionName, queryString));
            else logger.info(String.format("DB: %s, query: %s", collectionName, queryString));
        }
        if (!extraQueryStr.equals("")) logger.info(String.format("DB: %s, extraQuery: %s\n", collectionName, extraQueryStr));

        // Patron para establecer si hay un filtro que apliquen sobre un JOIN anterior
        boolean hasJoinFilter = false;
        Pattern regex = Pattern.compile("join=([\\w,]+)&?");
        Matcher matcher = regex.matcher(queryString);
        if (matcher.find()) {
            String[] joinFields = matcher.group(1).split(",");
            for (String joinField : joinFields) {
                Pattern regexField = Pattern.compile(
                    String.format("join=[\\w,]*%1$s[\\w,]*.*[&|(]filter:\\w+:%1$s\\.[\\w.]+=[\\w.]+", joinField)
                );
                if (regexField.matcher(queryString).find()) {
                    hasJoinFilter = true;
                    break;
                }
            }
        }

        // Inicializar arreglos auxiliares
        LinkedList<String> fieldList = new LinkedList<>();
        LinkedList<SortItem> sortList = new LinkedList<>();
        LinkedList<AggregationOperation> operations = new LinkedList<>();
        LinkedList<AggregationOperation> operationsCount = new LinkedList<>();
        LinkedList<String> joinList;

        // Variables auxiliares
        AggregationOperation aggregation;

        // Dividir por & (AND)
        String[] queriesString = queryString.equals("") ? null : queryString.split("&");

        // Dividir por & (AND) las consultas realizadas por el Backend
        String[] extraQueriesString = extraQueryStr.equals("") ? null : extraQueryStr.split("&");

        // Separar las consultas
        for (int i = 0; i < 2; i++) {
            String[] strList;
            QueryFilterEnable filterEnable;

            // String de filtro auxiliar para corregir la precedencia de OR y AND
            StringBuilder filters = new StringBuilder();

            if (i == 0 && extraQueriesString != null) {
                strList = extraQueriesString;
                filterEnable = QueryFilterEnable.enable();
            } else if (queriesString != null) {
                strList = queriesString;
                filterEnable = options.getEnableConf();
            } else {
                continue;
            }

            // Iteración sobre las partes de AND
            for (String qStr : strList) {
                if (qStr.equals("")) {
                    continue;
                }
                // Si no es un filtro y hay filtros para procesar
                if (!qStr.matches("^[(!]*filter.*") && !filters.toString().equals("") && filterEnable.isEnableFilter()) {
                    Criteria c = manageFilter(filters.toString());
                    if (c != null) {
                        aggregation = Aggregation.match(c);
                        operations.add(aggregation);
                        operationsCount.add(aggregation);
                    }

                    filters = new StringBuilder();
                }

                // Si es de paginación
                if (qStr.startsWith("page=") && filterEnable.isEnablePage()) {
                    managePage(qStr, result);

                    if (result.pageStart != null && result.pageSize != null) {
                        operations.add(Aggregation.skip((long) result.pageStart));
                        operations.add(Aggregation.limit((long) result.pageSize));
                    }
                }

                // Si es total
                else if (qStr.startsWith("total") && filterEnable.isEnableTotal()) {
                    manageTotal(result);
                    operationsCount.add(Aggregation.count().as("count"));
                }

                // Si es un filtro
                else if (qStr.matches("^[(!]*filter.*") && filterEnable.isEnableFilter()) {
                    // Agregar a una variable temporal los filtros
                    // para luego poder procesar correctamente los AND y OR
                    if (filters.toString().equals("")) {
                        filters = new StringBuilder(qStr);
                    } else {
                        filters.append("&").append(qStr);
                    }
                }

                // Si es un join
                else if (qStr.startsWith("join=") && filterEnable.isEnableJoin()) {
                    joinList = new LinkedList<>();
                    manageJoin(qStr, joinList);
                    String table;
                    List<String> allowedList = filterEnable.getJoinAllowedList();

                    for (String join : joinList) {
                        // Buscar el nombre de la tabla para unir por la etiqueta @Document de MongoDB

                        if (allowedList != null && !allowedList.contains(join)) {
                            continue;
                        }

                        try {
                            // Obtener el path de la case de la variable a unir.
                            String definition = ((ParameterizedType) dataClass.getDeclaredField(join).getGenericType())
                                .getActualTypeArguments()[0].getTypeName();
                            Document doc = Class.forName(definition).getAnnotation(Document.class);

                            // Obtener el nombre de la tabla de la etiqueta.
                            table = doc.collection();
                        } catch (Exception e) {
                            continue;
                        }

                        aggregation = Aggregation.lookup(table, join + "._id", "_id", join);
                        operations.add(aggregation);

                        // Solo agrega el JOIN al count si se realiza un
                        // filtro dentro de un elemento JOIN
                        if (hasJoinFilter) {
                            operationsCount.add(aggregation);
                        }
                    }
                }

                // Si es un sort
                else if (qStr.startsWith("sort=") && filterEnable.isEnableSort()) {
                    manageSort(qStr, sortList);

                    for (SortItem sortItem : sortList) {
                        operations.add(Aggregation.sort(sortItem.direction, sortItem.field));
                    }
                }

                // Si es un filtrado de campos
                else if (qStr.startsWith("fields=") && filterEnable.isEnableFields()) {
                    fieldList = new LinkedList<>();
                    manageFields(qStr, fieldList);
                }
            }

            // Comprobar que no queden filtros por realizar
            if (!filters.toString().equals("")) {
                Criteria c = manageFilter(filters.toString());
                if (c != null) {
                    aggregation = Aggregation.match(c);
                    operations.add(aggregation);
                    operationsCount.add(aggregation);
                }
            }
        }

        // Ejecutar la consulta
        ExecuteQueryInternal(result, mongoTemplate, fieldList, operations, dataClass, operationsCount, collectionName);
        return result;
    }

    private void managePage(String qStr, PageData<T> result) {
        String[] parts = qStr.split("=");
        if (parts.length != 2) {
            return;
        }

        String[] stringPage = parts[1].split(":");
        if (stringPage.length > 1) {
            int start = Integer.parseInt(stringPage[0]);
            int size = Integer.parseInt(stringPage[1]);
            result.pageStart = start;
            result.pageSize = size;

        } else if (stringPage.length == 1) {
            int size = Integer.parseInt(stringPage[0]);

            if (size > 0) {
                result.pageStart = 0;
                result.pageSize = size;
            }
        }
    }

    private void manageTotal(PageData<T> result) {
        result.pageStart = 0;
        result.pageSize = 0;
    }

    private void manageFields(String qStr, List<String> fields) {
        try {
            String[] fieldList = qStr.split("=")[1].split(",");
            Collections.addAll(fields, fieldList);
        } catch (Exception ignored) {
        }
    }

    private Criteria manageFilter(String qStr) {
        List<String> orList = Utils.splitGreaterLevel(qStr, '|', '(', ')', true);

        Criteria criteria = new Criteria();
        Criteria criteriaAux;
        List<Criteria> orCritters = new LinkedList<>();
        boolean negate = false;

        for (String orPartStr : orList) {
            List<String> andList = Utils.splitGreaterLevel(orPartStr, '&', '(', ')', true);
            List<Criteria> andCritters = new LinkedList<>();

            for (String andPartStr : andList) {
                while (andPartStr.startsWith("!")){
                    negate = !negate;
                    andPartStr = andPartStr.substring(1);
                }
                if (andPartStr.startsWith("(")) {

                    criteriaAux = manageFilter(andPartStr.split("^\\(")[1].split("\\)$")[0]);
                    if(criteriaAux != null && negate) {
                        criteriaAux = new Criteria().norOperator(criteriaAux);
                        negate = false;
                    }

                    andCritters.add(criteriaAux);
                    continue;
                }

                String[] parts = andPartStr.split("=");
                if (parts.length != 2) {
                    continue;
                }

                String[] options = parts[0].split(":");
                if (options.length < 2) {
                    continue;
                }

                String valueStr = parts[1];
                Object value;

                // Casteo dinámico de partes
                if (!options[FILTER_SYNTAX.OPERATION.ordinal()].equals("IN")) {
                    if (options.length > FILTER_SYNTAX.CAST_TYPE.ordinal()) {
                        String type = options[FILTER_SYNTAX.CAST_TYPE.ordinal()];
                        try {
                            value = DynamicTypes.castType(type, valueStr);
                        } catch (BadRequestException requestException) {
                            throw new BadRequestException(requestException.getMessage() +
                                " on " + qStr);
                        }
                    } else {
                        value = DynamicTypes.inferType(valueStr);
                    }
                } else {
                    value = "";
                }

                OPERATIONS operation;
                try{
                    operation = OPERATIONS.valueOf(options[FILTER_SYNTAX.OPERATION.ordinal()]);
                }catch (Exception e){
                    throw new BadRequestException("Invalid operation on " + qStr);
                }

                if(
                    !operation.equals(OPERATIONS.EQ) &&
                    !operation.equals(OPERATIONS.NEQ) &&
                    value == null
                ){
                    throw new BadRequestException("null value only with 'EQ' or 'NEQ' operations");
                }

                criteriaAux = Criteria.where(options[FILTER_SYNTAX.FIELD.ordinal()]);

                switch (operation) {
                    case REGEX:
                        if(value == null) throw new BadRequestException("REGEX operation invalid with null, convert to string if necessary");
                        criteriaAux = criteriaAux.regex(value.toString());
                        break;

                    case EQ:
                        criteriaAux = criteriaAux.is(value);
                        break;

                    case GT:
                        criteriaAux = criteriaAux.gt(value);
                        break;

                    case GTE:
                        criteriaAux = criteriaAux.gte(value);
                        break;

                    case LT:
                        criteriaAux = criteriaAux.lt(value);
                        break;

                    case LTE:
                        criteriaAux = criteriaAux.lte(value);
                        break;

                    case NEQ:
                        criteriaAux = criteriaAux.ne(value);
                        break;

                    case IN:
                        if (options.length <= FILTER_SYNTAX.CAST_TYPE.ordinal()) {
                            throw new BadRequestException("Missing data type on " + qStr);
                        }

                        // Escapar las comas( , )
                        String[] values = Pattern.compile("(?<!\\\\),").split(valueStr);

                        // Transformar los valores a su tipo correcto especificado
                        ArrayList<Object> objects = new ArrayList<>(values.length);
                        String type = options[FILTER_SYNTAX.CAST_TYPE.ordinal()];

                        for (String valStr : values) {
                            objects.add(DynamicTypes.castType(type, valStr));
                        }
                        criteriaAux = criteriaAux.in(objects);
                        break;
                }

                if(negate) {
                    criteriaAux = new Criteria().norOperator(criteriaAux);
                    negate = false;
                }
                andCritters.add(criteriaAux);
            }

            if (andCritters.size() == 1) {
                orCritters.add(andCritters.get(0));
            } else if (andCritters.size() > 1) {
                orCritters.add(new Criteria().andOperator(andCritters.toArray(new Criteria[0])));
            }
        }

        if (orCritters.size() > 1) {
            criteria.orOperator(orCritters.toArray(new Criteria[0]));
            return criteria;
        } else if (orCritters.size() == 1) {
            return orCritters.get(0);
        } else {
            return null;
        }
    }

    private void manageJoin(String qStr, List<String> joinList) {
        String[] parts = qStr.split("=");
        if (parts.length == 2) {
            String[] fields = parts[1].split(",");
            Collections.addAll(joinList, fields);
        }
    }

    private void manageSort(String qStr, LinkedList<SortItem> sortList) {
        String[] parts = qStr.split("=");
        if (parts.length == 2) {
            // Dividir por comas
            String[] sortsFields = parts[1].split(",");
            String[] partsSort;

            // Recorrer todos los campos
            for (String field : sortsFields) {

                // Dividir para saber si es DESC o ASC
                partsSort = field.split(":");

                if (partsSort.length == 1) {
                    // Por defecto es DESC en MongoDB
                    sortList.addFirst(new SortItem(partsSort[0]));
                } else if (partsSort.length == 2) {

                    if (partsSort[1].equals("DESC")) {

                        // Por defecto es DESC em SortItem, no hace falta especificar
                        sortList.addFirst(new SortItem(partsSort[0]));
                    } else if (partsSort[1].equals("ASC")) {
                        sortList.addFirst(new SortItem(partsSort[0], Sort.Direction.ASC));
                    }
                }
            }
        }
    }

    private void ExecuteQueryInternal(
        PageData<T> result, MongoTemplate mongoTemplate, List<String> fieldList,
        List<AggregationOperation> operations, Class<T> dataClass,
        List<AggregationOperation> operationsCount, String collectionName
    ) {
        // Si no hay operaciones se busca en toda la lista simplemente.
        if (operations.size() == 0 && operationsCount.size() == 0 && fieldList.size() == 0) {
            result.data = mongoTemplate.findAll(dataClass);
            return;
        }

        // Agregar el filtrado por campo
        if (fieldList.size() > 0) {
            ProjectionOperation projectionOperation = Aggregation.project(fieldList.toArray(new String[0]));
            if (!fieldList.contains("id")) {
                projectionOperation = projectionOperation.andExclude("_id");
            }
            operations.add(projectionOperation);
        }

        // Total
        if (result.pageSize != null && result.pageStart != null && result.pageSize == 0) {

            // Agregar el total si tiene paginación
            operationsCount.add(Aggregation.project("count"));

            TypedAggregation<CountModel> aggregation = Aggregation.newAggregation(
                CountModel.class,
                operationsCount.toArray(new AggregationOperation[0])
            );

            AggregationResults<CountModel> aggregateResultsCount = mongoTemplate.aggregate(
                aggregation, collectionName, CountModel.class
            );

            List<CountModel> mappedResults = aggregateResultsCount.getMappedResults();
            if (mappedResults.size() > 0) {
                result.total = mappedResults.get(0).count;
            } else {
                result.total = (long) 0;
            }

            result.pageSize = null;
            result.pageStart = null;
            return;
        }

        // obtener los resultados
        if (operations.size() > 0 && (result.pageSize == null || result.pageSize > 0)) {
            // Buscar los datos
            TypedAggregation<T> aggregation = Aggregation.newAggregation(
                dataClass,
                operations.toArray(new AggregationOperation[0])
            );
            try {
                AggregationResults<T> aggregateResults = mongoTemplate.aggregate(aggregation, dataClass);
                result.data = aggregateResults.getMappedResults();
            } catch (Exception e) {
                if (e.getMessage().contains("on class")) {
                    throw new BadRequestException(e.getMessage().split(" on class")[0]);
                }
                throw e;
            }
        } else {
            result.pageSize = null;
            result.pageStart = null;
        }
    }
}