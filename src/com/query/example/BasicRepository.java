package com.query.example;

import com.mongodb.client.result.DeleteResult;
import com.query.exception.BadRequestException;
import com.query.model.PageData;
import com.query.repository.QueryBuilder;
import com.query.repository.QueryBuilderOptions;
import com.query.repository.QueryBuilderStr;
import com.query.repository.QueryFilterEnable;
import com.query.util.DynamicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

public class BasicRepository<T> {
    @Autowired
    protected MongoTemplate mongoTemplate;

    protected QueryBuilder<T> queryBuilder;


    public void _init(String collationName, Class<T> dataClass) {
        queryBuilder = new QueryBuilder<>(mongoTemplate, dataClass, collationName);
    }

    public T save(T obj) {
        if(obj == null) return null;
        return mongoTemplate.save(obj);
    }

    public boolean saveAll(List<T> objList) {
        T aux;
        for (T elem : objList) {
            aux = this.save(elem);
            if (aux == null) {
                return false;
            }
        }
        return true;
    }

    public PageData<T> getAll(QueryFilterEnable enableConf){
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setEnableConf(enableConf)
        );
    }


    public PageData<T> getById(String id, QueryFilterEnable enableConf) throws BadRequestException {
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setEnableConf(enableConf)
            .setExtraQuery(QueryBuilderStr.build()
                .filter("_id", QueryBuilderStr.FILTER_OP.EQ, id, DynamicTypes.TYPES.OBJECT_ID)
            )
        );
    }

    public boolean deleteById(String id) {
        PageData<T> objOP = this.getById(id, QueryFilterEnable.disable());
        if (!objOP.hasData()) {
            return false;
        }
        DeleteResult res = mongoTemplate.remove(objOP.get());
        return res.getDeletedCount() > 0;
    }

}
