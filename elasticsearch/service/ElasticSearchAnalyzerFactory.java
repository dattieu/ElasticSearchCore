package com.wse.common.elasticsearch.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.util.CollectionUtils;

import com.wse.common.elasticsearch.service.ElasticSearchAnalyzer.Analyzer;
import com.wse.common.elasticsearch.service.ElasticSearchAnalyzer.CustomAnalyzer;

public final class ElasticSearchAnalyzerFactory {
    
    public static final ElasticSearchAnalyzer getAnalyzer(AnalyzerType analyzerType) {
        ElasticSearchAnalyzer analysis = new ElasticSearchAnalyzer();
        Analyzer analyzer = new Analyzer();
        CustomAnalyzer customAnalyzer = new CustomAnalyzer();
        
        switch(analyzerType) {
            case DEFAULT:
            default:
                configAnalyzerProperties(customAnalyzer, Collections.emptyList());
                break;
            case AUTO_COMPLETE: // TODO
                configAnalyzerProperties(customAnalyzer, Arrays.asList("autocomplete_filter"));
                break;
        }
        
        analyzer.setCustomAnalyzer(customAnalyzer);
        analysis.setAnalyzer(analyzer);
        return analysis;
    }
    
    private static void configAnalyzerProperties(CustomAnalyzer customAnalyzer, List<String> customFilters) {
        List<String> tokenFilter = buildTokenFilters(customFilters);
        customAnalyzer.setTokenizer(Tokenizer.KEYWORD.tokenName);
        customAnalyzer.setFilter(tokenFilter.toArray(new String[tokenFilter.size()]));
    }
    
    private static List<String> buildTokenFilters(List<String> customFilters) {
        List<String> filters = new ArrayList<>(Arrays.asList(TokenFilter.ASCII_FOLDING.filterName, TokenFilter.LOWERCASE.filterName));
        if (!CollectionUtils.isEmpty(customFilters)) {
            filters.addAll(customFilters);
            return filters;
        }
        
        filters.add(TokenFilter.STANDARD.filterName);
        return filters;
    }
    
    public static enum AnalyzerType {
        DEFAULT, AUTO_COMPLETE
    }
    
    public static enum Tokenizer {
        STANDARD("standard"), KEYWORD("keyword"), SIMPLE("simple"), LETTER("letter");

        private String tokenName;
        
        Tokenizer(String tokenName) {
            this.tokenName = tokenName;
        }
        
        public String tokenName() {
            return tokenName;
        }
    }
    
    public static enum TokenFilter {
        STANDARD("standard"), ASCII_FOLDING("asciifolding"), LOWERCASE("lowercase");
        
        private String filterName;
        
        TokenFilter(String filterName) {
            this.filterName = filterName;
        }
        
        public String filterName() {
            return filterName;
        }
    }
}
