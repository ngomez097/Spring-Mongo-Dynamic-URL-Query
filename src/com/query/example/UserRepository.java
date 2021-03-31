package com.query.example;

import com.query.exception.BadRequestException;
import com.query.example.BasicRepository;
import com.query.model.PageData;
import com.query.repository.QueryBuilderOptions;
import com.query.repository.QueryBuilderStr;
import com.query.repository.QueryBuilderStr.FILTER_OP;
import com.query.repository.QueryFilterEnable;
import com.query.util.DynamicTypes;
import com.query.util.DynamicTypes.TYPES;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;

@Service
public class UserRepository extends BasicRepository<User> {

    @PostConstruct
    public void init() {
        this._init(Constants.COLLECTION_USER, User.class);
    }

    public PageData<User> getUsers() throws BadRequestException {
        return queryBuilder.executeQuery();
    }

    public PageData<User> getUserById(String id, QueryFilterEnable enableConf) throws BadRequestException {
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setExtraQuery(
                QueryBuilderStr.build()
                    .filter("_id", QueryBuilderStr.FILTER_OP.EQ, id, TYPES.OBJECT_ID)
            )
            .setEnableConf(enableConf)
        );
    }

    public PageData<User> getById(String id, QueryFilterEnable enableConf) throws BadRequestException {
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setExtraQuery(
                QueryBuilderStr.build()
                    .filter("_id", QueryBuilderStr.FILTER_OP.EQ, id, TYPES.OBJECT_ID)
            ).setEnableConf(enableConf)
        );
    }

    public PageData<User> getByEmail(String email, QueryFilterEnable enableConf) throws BadRequestException {
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setExtraQuery(
                QueryBuilderStr.build()
                    .filter("email", QueryBuilderStr.FILTER_OP.EQ, email, TYPES.STRING)
            ).setEnableConf(enableConf)
        );
    }

    public PageData<User> getByCuil(String cuil, QueryFilterEnable enableConf) {
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setExtraQuery(
                QueryBuilderStr.build()
                    .filter("cuil", QueryBuilderStr.FILTER_OP.EQ, cuil, TYPES.LONG)
            ).setEnableConf(enableConf)
        );
    }

    public PageData<User> getByEmailOrCuilOrDni(String email,Long cuil,Long dni, QueryFilterEnable enableConf) {
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setExtraQuery(
                QueryBuilderStr.build()
                    .filter("email", QueryBuilderStr.FILTER_OP.EQ, email, TYPES.STRING)
                    .or()
                    .filter("cuil", FILTER_OP.EQ, cuil.toString(),TYPES.LONG)
                    .or()
                    .filter("dni", FILTER_OP.EQ, dni.toString(),TYPES.LONG)
            ).setEnableConf(enableConf)
        );
    }

    public PageData<User> getByAccount_id(String account_id, QueryFilterEnable enableConf) {
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setExtraQuery(
                QueryBuilderStr.build()
                    .filter("account_id", QueryBuilderStr.FILTER_OP.EQ, account_id, TYPES.STRING)
            ).setEnableConf(enableConf)
        );
    }

    public PageData<User> getByIdAndInternalNull(String userID, QueryFilterEnable enableConf) throws BadRequestException {
        return queryBuilder.executeQuery(QueryBuilderOptions.build()
            .setExtraQuery(QueryBuilderStr.build()
                .filter("_id", QueryBuilderStr.FILTER_OP.EQ, userID, DynamicTypes.TYPES.OBJECT_ID)
                .and()
                .filter("internal", FILTER_OP.EQ, null)
            )
            .setEnableConf(enableConf)
        );
    }
}
