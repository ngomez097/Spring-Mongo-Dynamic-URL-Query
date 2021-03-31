package com.query.repository;

import java.util.Arrays;
import java.util.List;

public class QueryFilterEnable {
    private boolean enableJoin;
    private boolean enablePage;
    private boolean enableFilter;
    private boolean enableSort;
    private boolean enableTotal;
    private boolean enableFields;
    private List<String> joinAllowedList;


    private QueryFilterEnable(boolean initVal) {
        enableJoin = initVal;
        enablePage = initVal;
        enableFilter = initVal;
        enableSort = initVal;
        enableTotal = initVal;
        enableFields = initVal;
        joinAllowedList = null;
    }

    public static QueryFilterEnable enable() {
        return new QueryFilterEnable(true);
    }

    public static QueryFilterEnable disable() {
        return new QueryFilterEnable(false);
    }

    public QueryFilterEnable disableJoin() {
        this.enableJoin = false;
        return this;
    }

    public QueryFilterEnable enableJoin() {
        this.enableJoin = true;
        return this;
    }

    public QueryFilterEnable enableJoin(String... joinAllowedList) {
        this.enableJoin = true;
        this.joinAllowedList = Arrays.asList(joinAllowedList);
        return this;
    }

    public QueryFilterEnable disableFilter() {
        this.enableFilter = false;
        return this;
    }

    public QueryFilterEnable enableFilter() {
        this.enableFilter = true;
        return this;
    }

    public QueryFilterEnable disablePage() {
        this.enablePage = false;
        return this;
    }

    public QueryFilterEnable enablePage() {
        this.enablePage = true;
        return this;
    }

    public QueryFilterEnable disableSort() {
        this.enableSort = false;
        return this;
    }

    public QueryFilterEnable enableSort() {
        this.enableSort = true;
        return this;
    }

    public QueryFilterEnable disableTotal() {
        this.enableTotal = false;
        return this;
    }

    public QueryFilterEnable enableTotal() {
        this.enableTotal = true;
        return this;
    }

    public QueryFilterEnable disableFields() {
        this.enableFields = false;
        return this;
    }

    public QueryFilterEnable enableFields() {
        this.enableFields = true;
        return this;
    }

    public boolean isAnyEnable() {
        return enableFilter || enableTotal || enablePage || enableSort || enableJoin || enableFields;
    }

    public boolean isEnableJoin() {
        return enableJoin;
    }

    public boolean isEnablePage() {
        return enablePage;
    }

    public boolean isEnableFilter() {
        return enableFilter;
    }

    public boolean isEnableSort() {
        return enableSort;
    }

    public boolean isEnableTotal() {
        return enableTotal;
    }

    public boolean isEnableFields() {
        return enableFields;
    }

    public List<String> getJoinAllowedList() {
        return joinAllowedList;
    }
}
