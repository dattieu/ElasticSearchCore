package com.wse.common.elasticsearch.exception;

public class ElasticSearchServiceException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public ElasticSearchServiceException(String message) {
        super(message);
    }

}
