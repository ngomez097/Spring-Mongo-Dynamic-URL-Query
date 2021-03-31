package com.query.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Iterator;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageData<T> implements Iterable<T> {
    public List<T> data;

    public Integer pageStart;
    public Integer pageSize;

    public Long total;

    public PageData() {
    }

    public PageData(List<T> data) {
        this.data = data;
    }

    public PageData(List<T> data, Integer pageStart, Long total) {
        this.data = data;
        this.pageStart = pageStart;
        this.total = total;
    }

    @JsonIgnore
    public boolean hasData() {
        return this.data != null && this.data.size() > 0;
    }

    @JsonIgnore
    public boolean isEmpty(){
        return this.data == null || this.data.size() == 0;
    }

    public T get(int index) {
        if(data == null || data.size() <= index){
            return null;
        }
        return data.get(index);
    }

    public T get() {
        if(data == null || data.size() == 0){
            return null;
        }
        return data.get(0);
    }

    public int size() {
        return data.size();
    }


    @Override
    public Iterator<T> iterator() {
        if (data == null) {
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public T next() {
                    return null;
                }
            };
        }
        return data.iterator();
    }

}

