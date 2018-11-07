package com.wse.common.elasticsearch.service;

import java.util.Arrays;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;

public class SearchQueryData {
    
    private List<SearchParams> searchParams;
    
    private int from;
    
    private int size;
    
    private SortParams sortBy;

    public List<SearchParams> getSearchParams() {
        return searchParams;
    }

    public int getFrom() {
        return from;
    }

    public SearchQueryData setFrom(int from) {
        this.from = from;
        return this;
    }

    public int getSize() {
        return size;
    }

    public SearchQueryData setSize(int size) {
        this.size = size;
        return this;
    }
    
    public SearchQueryData setSearchParams(List<SearchParams> searchParams) {
        this.searchParams = searchParams;
        return this;
    }

    public SortParams getSortBy() {
        return sortBy;
    }

    public SearchQueryData setSortBy(SortParams sortBy) {
        this.sortBy = sortBy;
        return this;
    }

    public static class SearchParams {
        // TODO this kind of search parameters is not so flexible
        private String[] searchParams;
        private String condition;
        private SearchType searchType;

        public String[] getSearchParams() {
            return searchParams;
        }

        public SearchParams setSearchParams(String[] searchParams) {
            this.searchParams = searchParams;
            return this;
        }

        public String getCondition() {
            return condition;
        }

        public SearchParams setCondition(String condition) {
            this.condition = condition;
            return this;
        }

        public SearchType getSearchType() {
            return searchType;
        }

        public SearchParams setSearchType(SearchType searchType) {
            this.searchType = searchType;
            return this;
        }

        @Override
        public String toString() {
            return "SearchParams [searchParams=" + Arrays.toString(searchParams) + ", condition=" + condition
                    + ", searchType=" + searchType + "]";
        }
    }
    
    public static class SortParams {
        private String sortBy;
        private SortOrder order;
        
        public SortParams() {}
        
        public SortParams(String sortBy, SortOrder order) {
            super();
            this.sortBy = sortBy;
            this.order = order;
        }
        
        public String getSortBy() {
            return sortBy;
        }
        
        public SortParams setSortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }
        
        public SortOrder getOrder() {
            return order;
        }
        
        public SortParams setOrder(SortOrder order) {
            this.order = order;
            return this;
        }
    }
    
    public static enum SearchType {
        EXACT_MATCH, PREFIX_MATCH, MULTI_MATCH, EXIST_MATCH, RANGE_MATCH
    }
    
    public static enum SearchCondition {
        AND, OR, NOT, FILTER
    }

}
