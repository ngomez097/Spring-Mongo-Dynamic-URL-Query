package com.signbox.firmatura.repository.query;

public class QueryBuilderOptions {
    public QueryBuilderStr extraQuery;
    public QueryFilterEnable enableConf;

    public QueryBuilderOptions() {
    }

    public static QueryBuilderOptions build() {
        return new QueryBuilderOptions();
    }

    public QueryBuilderOptions setExtraQuery(QueryBuilderStr queryBuilderStr) {
        this.extraQuery = queryBuilderStr;
        return this;
    }

    public QueryBuilderOptions setEnableConf(QueryFilterEnable enableConf) {
        this.enableConf = enableConf;
        return this;
    }

    public QueryFilterEnable getEnableConf() {
        return this.enableConf;
    }

    public QueryBuilderStr getExtraQuery() {
        return extraQuery;
    }
}