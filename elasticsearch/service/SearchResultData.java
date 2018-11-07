package com.wse.common.elasticsearch.service;

import java.util.List;

public class SearchResultData<E> {
    
    private List<E> resultList;
    
    private long numberOfRecords;

    public List<E> getResultList() {
        return resultList;
    }

    public void setResultList(List<E> resultList) {
        this.resultList = resultList;
    }

    public long getNumberOfRecords() {
        return numberOfRecords;
    }

    public void setNumberOfRecords(long numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

}
