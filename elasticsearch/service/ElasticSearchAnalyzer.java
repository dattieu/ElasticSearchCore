package com.wse.common.elasticsearch.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("analysis")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public class ElasticSearchAnalyzer {

    private Analyzer analyzer;
    
    @JsonInclude(Include.NON_NULL)
    private Filter filter;
    
    public Analyzer getAnalyzer() {
        return analyzer;
    }
    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }
    public Filter getFilter() {
        return filter;
    }
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public static class Analyzer {
        // TODO this can be only used for the default
        @JsonProperty("default")
        private CustomAnalyzer customAnalyzer;

        public CustomAnalyzer getCustomAnalyzer() {
            return customAnalyzer;
        }
        public void setCustomAnalyzer(CustomAnalyzer customAnalyzer) {
            this.customAnalyzer = customAnalyzer;
        }
    }
    
    public static class CustomAnalyzer {
        private String[] filter;
        private String tokenizer;
        
        @JsonInclude(Include.NON_NULL)
        private String type;
        
        public String[] getFilter() {
            return filter;
        }
        public void setFilter(String[] filter) {
            this.filter = filter;
        }
        public String getTokenizer() {
            return tokenizer;
        }
        public void setTokenizer(String tokenizer) {
            this.tokenizer = tokenizer;
        }
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
    }
    
    public static class Filter {
        private String type;
        private int minGram;
        private int maxGram;
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public int getMinGram() {
            return minGram;
        }
        public void setMinGram(int minGram) {
            this.minGram = minGram;
        }
        public int getMaxGram() {
            return maxGram;
        }
        public void setMaxGram(int maxGram) {
            this.maxGram = maxGram;
        }
    }
    
}
