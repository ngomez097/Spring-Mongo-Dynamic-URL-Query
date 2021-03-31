package com.signbox.firmatura.repository.query;

import org.springframework.data.domain.Sort;

public class SortItem {
    public final String field;
    public final Sort.Direction direction;

    public SortItem(String field, Sort.Direction direction) {
        this.field = field;
        this.direction = direction;
    }

    public SortItem(String field) {
        this.field = field;
        this.direction = Sort.Direction.DESC;
    }
}