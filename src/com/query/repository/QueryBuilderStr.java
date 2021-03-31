package com.signbox.firmatura.repository.query;

import com.signbox.firmatura.util.DynamicTypes;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Iterator;

public class QueryBuilderStr {
    public final StringBuilder query;

    private QueryBuilderStr() {
        query = new StringBuilder();
    }

    public static QueryBuilderStr build() {
        return new QueryBuilderStr();
    }

    /**
     * Este método se utiliza para obtener la query formada.
     */
    public String getQuery() {
        return query.toString();
    }

    /**
     * Este método sirve para crear un filtro de paginación
     * marcando el inicio y la cantidad de elementos
     */
    public QueryBuilderStr page(Integer start, Integer limit) {
        this.insertAndIfNeeded();
        this.query.append(String.format("page=%d:%d", start, limit));
        return this;
    }

    /**
     * Este método sirve para crear un filtro de paginación
     * marcando la cantidad de elementos desde el inicio (0)
     */
    public QueryBuilderStr page(Integer limit) {
        this.insertAndIfNeeded();
        this.query.append(String.format("page=%d", limit));
        return this;
    }

    /**
     * Este método se utiliza para crear un filtro que
     * devuelva el total de la query únicamente.
     */
    public QueryBuilderStr total() {

        this.insertAndIfNeeded();
        this.query.append("total");
        return this;
    }

    /**
     * Este método se utiliza para crear un filtro sobre
     * un campo con inferencia de tipo de dato.
     */
    public QueryBuilderStr filter(String field, FILTER_OP operation, String value) {
        int size = this.query.length();
        char lastCharQuery = size != 0 ? this.query.charAt(size - 1) : '&';

        if(lastCharQuery != '&' && lastCharQuery != '|' && lastCharQuery != '('){
            this.query.append('&');
        }

        if(value == null) value = "null";


        this.query.append(String.format("filter:%s:%s=%s", operation, field, value));
        return this;
    }

    /**
     * Este método se utiliza para crear un filtro sobre
     * un campo especificando el tipo de dato.
     */
    public QueryBuilderStr filter(String field, FILTER_OP operation, String value, DynamicTypes.TYPES type) {
        int size = this.query.length();
        char lastCharQuery = size != 0 ? this.query.charAt(size - 1) : '&';

        if(lastCharQuery != '&' && lastCharQuery != '|' && lastCharQuery != '('){
            this.query.append('&');
        }

        if (value == null) {
            this.query.append(String.format("filter:%s:%s=%s", operation, field, "null"));
        } else {
            this.query.append(String.format("filter:%s:%s:%s=%s", operation, field, type.getValue(), value));
        }
        return this;
    }

    public QueryBuilderStr not(){
        this.query.append("!");
        return this;
    }

    public QueryBuilderStr encapsule(QueryBuilderStr queryBuilderStr) {
        int size = this.query.length();
        char lastCharQuery = size != 0 ? this.query.charAt(size - 1) : '&';

        if(lastCharQuery != '&' && lastCharQuery != '|' && lastCharQuery != '('){
            this.query.append('&');
        }

        this.query.append("(").append(queryBuilderStr.getQuery()).append(")");
        return this;
    }

    /**
     * Este método se utiliza para crear una operación de JOIN.
     */
    public QueryBuilderStr join(String... fields) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> it = Arrays.stream(fields).iterator();

        while (it.hasNext()) {
            builder.append(it.next()).append(",");
        }
        this.insertAndIfNeeded();
        this.query.append(String.format("join=%s", builder.deleteCharAt(builder.length() - 1)));
        return this;
    }

    /**
     * Este método se utiliza para crear una operación de filtrado
     * de campos que se obtendrán de la consulta.
     * Cuando se especifica un campo, el resto desaparecen del resultado.
     */
    public QueryBuilderStr fields(String... fields) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> it = Arrays.stream(fields).iterator();

        while (it.hasNext()) {
            builder.append(it.next()).append(",");
        }
        this.insertAndIfNeeded();
        this.query.append(String.format("fields=%s", builder.deleteCharAt(builder.length() - 1)));
        return this;
    }

    /**
     * Este método se utiliza para crear una operación de ordenamiento
     * por los campos dados, el primer campo dado tendrá mayor peso
     * que los campos siguientes
     */
    public QueryBuilderStr sort(SortItem... sortItems) {
        StringBuilder builder = new StringBuilder();
        Iterator<SortItem> it = Arrays.stream(sortItems).iterator();
        SortItem sortItem;
        String order;
        while (it.hasNext()) {
            sortItem = it.next();
            order = "";
            if (sortItem.direction.equals(Sort.Direction.ASC)) {
                order = ":ASC";
            }
            builder.append(sortItem.field).append(order).append(",");
        }

        this.insertAndIfNeeded();
        this.query.append(String.format("sort=%s", builder.deleteCharAt(builder.length() - 1)));
        return this;
    }

    /**
     * Este método se utiliza para unir varios filtros o operaciones.
     * Funciona como un una AND lógica si separa dos "filter".
     */
    public QueryBuilderStr and() {
        this.query.append("&");
        return this;
    }

    /**
     * Este método se utiliza para unir únicamente dos filtros
     * funcionando como una OR lógica.
     * El mal uso de este podría ocasionar inestabilidad en la consulta
     */
    public QueryBuilderStr or() {
        this.query.append("|");
        return this;
    }

    private void insertAndIfNeeded(){
        int size = this.query.length();
        if(size == 0) return;

        if(this.query.charAt(size - 1) != '&'){
            this.query.append('&');
        }
    }

    /**
     * Enum con las operaciones de filtros posible
     */
    public enum FILTER_OP {
        EQ, NEQ, GT, GTE, LT, LTE, REGEX, IN
    }
}