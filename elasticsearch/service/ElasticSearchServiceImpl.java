package com.wse.common.elasticsearch.service;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wse.common.elasticsearch.exception.ElasticSearchServiceException;
import com.wse.common.elasticsearch.helper.DateFormatter;
import com.wse.common.elasticsearch.service.ElasticSearchAnalyzerFactory.AnalyzerType;
import com.wse.common.elasticsearch.service.SearchQueryData.SearchCondition;
import com.wse.common.elasticsearch.service.SearchQueryData.SearchParams;
import com.wse.common.elasticsearch.service.SearchQueryData.SortParams;

public abstract class ElasticSearchServiceImpl<E> implements ElasticSearchService<E> {
	
	private static final String FIELD_TYPE = "type";
	private static final String FIELD_INDEX = "index";
	private static final String FIELD_TIMESTAMP = "updated_at";
	private static final String FIELD_DATE_FORMAT = "format";
	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private static final long DEFAULT_TIMEOUT_IN_MINUTES = 1;
	private static RefreshPolicy REFRESH_POLICY = RefreshPolicy.NONE;
	private static final int DEFAULT_CONFLICT_RETRY_TIMES = 3;
	
	private static final Gson GSON_MAPPER = new Gson();
    private static final ObjectMapper JACKSON_MAPPER = new ObjectMapper();
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchServiceImpl.class);

	private static enum FieldType {
	    TEXT("text"), LONG("long"), DATE("date"), INTEGER("integer"), TIMESTAMP("long"), OBJECT("object");
	    
	    private String type;
	    
	    FieldType(String type) {
	        this.type = type;
	    }
	    
	    public String getType() {
	        return type;
	    }
	}
	
	private static enum AliasRequestType {
	    ADD(AliasActions.ADD_ACTION), REMOVE(AliasActions.REMOVE_ACTION);
	    	    
	    private String jsonRequest;
	    
	    AliasRequestType(String jsonRequest) {
	        this.jsonRequest = jsonRequest;
	    }
	    
	    public String getJsonRequest() {
	        return jsonRequest;
	    }
	    
	    private static class AliasActions {
	        private static final String ALIAS_ENDPOINT = "_aliases";
	        
	        private static final String ADD_ACTION = "{\r\n" + 
	                "    \"actions\" : [\r\n" + 
	                "        { \"add\" : { \"index\" : \"sampleIndex\", \"alias\" : \"sampleAlias\" } }\r\n" + 
	                "    ]\r\n" + 
	                "}";
	        
	        private static final String REMOVE_ACTION = "{\r\n" + 
	                "    \"actions\" : [\r\n" + 
	                "        { \"remove\" : { \"index\" : \"sampleIndex\", \"alias\" : \"sampleAlias\" } }\r\n" + 
	                "    ]\r\n" + 
	                "}";
	    }
	    
	    private static enum HttpMethod {
	        GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");
	        
	        private String method;
	        
	        HttpMethod(String method) {
	            this.method = method;
	        }
	        
	        public String getMethod() {
	            return method;
	        }
	    }
	}
	

    private String indexName;
    
    private String documentType;
    
    @Autowired
    private RestHighLevelClient client;
    
    protected Class<? extends E> entityType;

    @SuppressWarnings("unchecked")
    public ElasticSearchServiceImpl() {
        ParameterizedType paramType = (ParameterizedType) getClass().getGenericSuperclass();
        entityType = (Class<? extends E>) paramType.getActualTypeArguments()[0];
    }
    
    private RestClient getLowLevelClient() {
        return client.getLowLevelClient();
    }
    
    @Override
    public void initData(final String indexName, final String documentType, final Object mappings, final Map<String, E> entityMapWithId) {
        if (existIndex()) {
            deleteIndex(indexName);
        }

        createIndexWithMappings(indexName, documentType, mappings);
        
        if (!CollectionUtils.isEmpty(entityMapWithId)) {
            bulkIndex(entityMapWithId, ServiceMode.ASYNC);
        }
    }
    
    @Override
    public void configureIndex(String indexName, String documentType) {
        this.indexName = indexName;
        this.documentType = documentType;
    }
    
    @Override
    public boolean existIndex(final String indexName, final String documentType) {
        configureIndex(indexName, documentType);
        return existIndex();
    }
    
    @Override
    public boolean existIndex() {
        try {
            return client.indices().exists(buildGetIndexRequest(indexName));
        } catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public boolean existDocument(final String indexName, final String documentType, final String documentId) {
        try {
            return client.exists(buildGetIndexRequest(indexName, documentType, documentId));
        }   
        catch (IOException e) {
            throw new ElasticSearchServiceException("The index: " + indexName + " does not exist.");
        }
    }

    private GetRequest buildGetIndexRequest(String indexName, final String documentType, final String documentId) { 
        return new GetRequest(indexName, documentType, documentId).fetchSourceContext(new FetchSourceContext(false))
                .storedFields("_none_"); // use only for checking document existence, so no need to fetch source
    }
    
    private GetIndexRequest buildGetIndexRequest(String indexName) { 
        return new GetIndexRequest().indices(indexName)
                .local(false)
                .humanReadable(false)
                .includeDefaults(false);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void createIndexWithMappings(final String indexName, final String documentType, final Object mappings) {
        if (existIndex()) {
            return;
        }
        if (mappings instanceof Map) {
            createIndexWithMappings(indexName, documentType, (Map<String, Map<String, Object>>) mappings);
        }
        else {
            createIndexWithMappings(indexName, documentType, (String) mappings);
        }
    }
    
    private void createIndexWithMappings(final String indexName, final String documentType, final Map<String, Map<String, Object>> mappings) {
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> typeMap = new HashMap<>();
        Map<String, Object> propertiesMap = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> field : mappings.entrySet()) {
            propertiesMap.put(field.getKey(), field.getValue());
        }
        
        typeMap.put("properties", propertiesMap);
        jsonMap.put(documentType, typeMap);
        createIndex(buildCreateIndexRequest(indexName, documentType, jsonMap));
    }
    
    private void createIndexWithMappings(final String indexName, final String documentType, final String jsonMappings) {
        createIndex(buildCreateIndexRequest(indexName, documentType, jsonMappings));
    }
    
    @Override
    public void createIndex(final String indexName) {
        createIndex(buildCreateIndexRequest(indexName));
    }
    
    private void createIndex(CreateIndexRequest createIndexRequest) {
        try {
            CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest);
            if (!createIndexResponse.isAcknowledged()) {
                throw new ElasticSearchServiceException("Unable to create the index: " + indexName);
            }
        }
        catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to create the index. Exception: " + e.getMessage());
        }
    }
    
    private CreateIndexRequest buildCreateIndexRequest(String indexName) {
        return new CreateIndexRequest(indexName).timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES));
    }
    
    private CreateIndexRequest buildCreateIndexRequest(String indexName, String documentType, Map<String, Object> mappings) {
        return buildCreateIndexRequest(indexName).mapping(documentType, mappings)
                .settings(buildDefaultIndexSettings(), XContentType.JSON);
    }
    
    private CreateIndexRequest buildCreateIndexRequest(String indexName, String documentType, String mappings) {
        return buildCreateIndexRequest(indexName).mapping(documentType, mappings, XContentType.JSON)
                .settings(buildDefaultIndexSettings(), XContentType.JSON);
    }
    
    
    private String buildDefaultIndexSettings() {
        try {
            return JACKSON_MAPPER.writeValueAsString(ElasticSearchAnalyzerFactory.getAnalyzer(AnalyzerType.DEFAULT));
        } catch (JsonProcessingException e) {
            throw new ElasticSearchServiceException("Unable to create index setting configuration for the index. Exception: " + e.getMessage());
        }
    }
    
    @Override
    public void createIndexMapping(final String indexName, final String documentType, final Object mappings) {
        PutMappingRequest request = new PutMappingRequest(indexName).type(documentType).timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES));
        request.source(mappings, XContentType.JSON);
        if (mappings instanceof Map) {
            request.source(mappings);
        }
        
        try {
            client.indices().putMapping(request);
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to create mappings for the index: " + indexName);
        }
    }
    
    @Override
    public void createIndexSetting(final String indexName, final String settings) {
        UpdateSettingsRequest request = new UpdateSettingsRequest(indexName)
                .settings(settings, XContentType.JSON)
                .timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES));
        try {
            client.indices().putSettings(request);
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to create settings for the index: " + indexName);
        }
    }
    
    @Override
    public void deleteIndex(final String indexName) {
        try {
            DeleteIndexResponse deleteIndexResponse = client.indices().delete(buildDeleteIndexRequest(indexName));
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new ElasticSearchServiceException("Unable to delete the index: " + indexName);
            }
        }
        catch (ElasticsearchException e) {
            commonElasticSearchException(e);
        }
        catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to delete the index. Exception" + e.getMessage());
        }

    }
    
    private DeleteIndexRequest buildDeleteIndexRequest(final String indexName) {
        return new DeleteIndexRequest(indexName).timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES));
    }
    
    @Override
    public void index(final E entity, final String id) {
        index(buildIndexRequest(entity, id));
    }
    
    private IndexRequest buildIndexRequest(E entity, String id) {
        return new IndexRequest(indexName, documentType, id)
                .source(GSON_MAPPER.toJson(entity), XContentType.JSON)
                .opType(DocWriteRequest.OpType.CREATE)
                .timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES))
                .setRefreshPolicy(REFRESH_POLICY);
    }
    
    private BeanInfo getBeanInfo(Class<? extends E> entityType) {
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(entityType);
        } 
        catch (IntrospectionException e) {
            throw new ElasticSearchServiceException("Unable to retrieve the information of the entity. Exception: " + e.getMessage());
        }
        return beanInfo;
    }
    
    @Override
    public void bulkIndex(final Map<String, E> entityMapWithId) {
        bulk(entityMapWithId, Collections.emptyMap(), Collections.emptyMap(), ServiceMode.SYNC);
    }
    
    @Override
    public void bulkIndex(final Map<String, E> entityMapWithId, final ServiceMode mode) {
        bulk(entityMapWithId, Collections.emptyMap(), Collections.emptyMap(), mode);
    }
    
    @Override
    public void bulk(final Map<String, E> entityMapToIndex, final Map<String, E> entityMapToDelete, 
            final Map<String, E> entityMapToUpdate, final ServiceMode mode) {
        BulkRequest bulkRequest = buildBulkRequest();
        
        if (!CollectionUtils.isEmpty(entityMapToIndex)) {
            bulkRequest = buildBulkRequest(entityMapToIndex, RequestType.INDEX, bulkRequest);
        }
        if (!CollectionUtils.isEmpty(entityMapToUpdate)) {
            bulkRequest = buildBulkRequest(entityMapToUpdate, RequestType.UPDATE, bulkRequest);
        }
        if (!CollectionUtils.isEmpty(entityMapToDelete)) {
            bulkRequest = buildBulkRequest(entityMapToDelete, RequestType.DELETE, bulkRequest);
        }
        
        if (ServiceMode.isSync(mode)) {
            bulk(bulkRequest);
            return;
        }
        bulkAsync(bulkRequest);
    }
    
    private BulkRequest buildBulkRequest(Map<String, E> entityMapWithId, RequestType requestType, BulkRequest bulkRequest) {
        bulkRequest = bulkRequest != null ? bulkRequest : buildBulkRequest();
        for (Map.Entry<String, E> entry : entityMapWithId.entrySet()) {
            bulkRequest.add(buildRequest(requestType, entry.getValue(), entry.getKey()));
        }
        return bulkRequest;
    }
    
    private BulkRequest buildBulkRequest() {
        return new BulkRequest().timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES))
                .setRefreshPolicy(REFRESH_POLICY);
    }
    
    @SuppressWarnings("rawtypes")
    private DocWriteRequest buildRequest(RequestType requestType, E entity, String entityId) {
        switch (requestType) {
            case INDEX:
                return buildIndexRequest(entity, entityId);
            case DELETE:
                return buildDeleteRequest(entityId);
            case UPDATE:
            default:
                return buildUpdateRequest(entity, entityId, true);
        }
    }
    
    private void bulk(BulkRequest bulkRequest) {
        try {
            client.bulk(bulkRequest);
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to make bulk operations. Exception: " + e.getMessage());
        }
    }
    
    private void bulkAsync(BulkRequest bulkRequest) {
        client.bulkAsync(bulkRequest, buildAsyncListener());
    }
    
    @Override
    public void update(final E entity, final String id) {
        upsert(buildUpdateRequest(entity, id, false));
    }
    
    @Override
    public void upsert(final E entity, final String id, final ServiceMode mode) {
        UpdateRequest upsertRequest = buildUpdateRequest(entity, id, true);
        if (ServiceMode.isSync(mode)) {
            upsert(upsertRequest);
            return;
        }
        upsertAsync(upsertRequest);
    }
    
    private UpdateRequest buildUpdateRequest(E entity, String id, boolean shouldUpsert) {
        return new UpdateRequest(indexName, documentType, id)
                .doc(GSON_MAPPER.toJson(entity), XContentType.JSON)
                .docAsUpsert(shouldUpsert)
                .timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES))
                .setRefreshPolicy(REFRESH_POLICY)
                .retryOnConflict(DEFAULT_CONFLICT_RETRY_TIMES);
    }
    
    private void upsert(UpdateRequest upsertRequest) {
        try {
            client.update(upsertRequest);
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to update the document. Exception: " + e.getMessage());
        }
    }

    private void upsertAsync(UpdateRequest upsertRequest) {
        client.updateAsync(upsertRequest, buildAsyncListener());
    }
    
    private void commonElasticSearchException(ElasticsearchException e) {
        if (e.status() == RestStatus.NOT_FOUND) {
            throw new ElasticSearchServiceException("The required entity not found. Exception: " + e.getDetailedMessage());
        }
        if (e.status() == RestStatus.CONFLICT) {
            throw new ElasticSearchServiceException("A confliction happened when updating the document. Exception: " + e.getDetailedMessage());
        }
    }

    @Override
    public void delete(final E entity, final String id) {
        delete(buildDeleteRequest(id));
    }
    
    @Override
    public void delete(final E entity, final String id, final ServiceMode mode) {
        DeleteRequest deleteRequest = buildDeleteRequest(id);
        if (ServiceMode.isSync(mode)) {
            delete(deleteRequest);
            return;
        }
        deleteAsync(buildDeleteRequest(id));
    }
    
    private DeleteRequest buildDeleteRequest(final String entityId) {
        return new DeleteRequest(indexName, documentType, entityId)
                .timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES))
                .setRefreshPolicy(REFRESH_POLICY);
    }
    
    private void delete(DeleteRequest deleteRequest) {
        try {
            client.delete(deleteRequest);
        }
        catch (ElasticsearchException e) {
            commonElasticSearchException(e);
        }
        catch (IOException e){
            throw new ElasticSearchServiceException("Unable to delete the entity document. Exception: " + e.getMessage());
        }
    } 
    
    private void deleteAsync(DeleteRequest deleteRequest) {
        client.deleteAsync(deleteRequest, buildAsyncListener());
    }

    @Override
    public long count(final SearchQueryData queryData) {
        try {
            return client.search(buildSearchRequest(queryData)).getHits().getTotalHits();
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to count the number of total documents. Exception: " + e.getMessage());
        }
    }
    
    @Override
    public long count(final JsonObject request) {
        String countEndpoint = String.format("/%s/_count", indexName);
        HttpEntity countEntity = new NStringEntity(GSON_MAPPER.toJson(request), ContentType.APPLICATION_JSON);
        
        try {
            Response response = getLowLevelClient().performRequest("POST", countEndpoint, new HashMap<String, String>(), countEntity);
            JsonNode responseBody = JACKSON_MAPPER.readTree(response.getEntity().getContent());
            return responseBody.path("count").asLong();
        } 
        catch (IOException exception) {
            throw new ElasticSearchServiceException("Unable to perform count operation. Exception: " + exception.getMessage());
        }
    }
    
    @Override
    public List<E> search() {
        return search(new SearchQueryData().setSize(0));
    }
    
    @Override
    public List<E> search(final String fieldToSearch, final String textToSearch) {
        String[] param = {fieldToSearch, textToSearch};
        SearchParams searchParam = new SearchParams().setSearchParams(param).setCondition("OR");
        SearchQueryData queryData = new SearchQueryData().setSearchParams(Arrays.asList(searchParam))
                                                     .setFrom(0).setSize(10);
        return search(queryData);
    }
    
    @Override
    public List<E> search(final SearchQueryData queryData) {
        try {
            return extractResultFromSearchResponse(client.search(buildSearchRequest(queryData)));
        } 
        catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to search. Exception: " + e.getMessage());
        }
    }
        
    private SearchRequest buildSearchRequest(SearchQueryData queryData) {
        return new SearchRequest().source(buildSearchSource(queryData)).indices(indexName);
    }
    
    private SearchSourceBuilder buildSearchSource(SearchQueryData queryData) {
        SearchSourceBuilder search = new SearchSourceBuilder();
        search.query(buildQuery(queryData))
            .sort(buildSortOption(queryData.getSortBy()))
            .from(queryData.getFrom())
            .size(queryData.getSize())
            .timeout(TimeValue.timeValueMinutes(DEFAULT_TIMEOUT_IN_MINUTES));
        return search;
    }
    
    private QueryBuilder buildQuery(SearchQueryData queryData) {
        if (CollectionUtils.isEmpty(queryData.getSearchParams())) {
            return QueryBuilders.matchAllQuery();
        }
        
        return getDisMaxQueryBuilder(queryData.getSearchParams());
    }
    
    // Basically, we should sort by default Elastic Search field like "_id"
    // If not, we should add "unmappedType" attribute (it 's not good for performance)
    // Default, sort by date time (if documents have a date time field)
    private FieldSortBuilder buildSortOption(SortParams sortParams) {
        if (sortParams != null) {
            if (!StringUtils.isEmpty(sortParams.getSortBy())) {
                return new FieldSortBuilder(sortParams.getSortBy()).order(sortParams.getOrder());
            }
        }
          
        return new FieldSortBuilder(FIELD_TIMESTAMP).unmappedType(FieldType.LONG.getType()).order(SortOrder.DESC);
    }
    
    private DisMaxQueryBuilder getDisMaxQueryBuilder(List<SearchParams> searchParamsList) {
        DisMaxQueryBuilder queryList = QueryBuilders.disMaxQuery();
    	BoolQueryBuilder boolQuery =  QueryBuilders.boolQuery();

        for (SearchParams params : searchParamsList) {
            queryList.add(getBoolQueryBuilder(boolQuery, params));
        }
        return queryList;
    }
    
    private BoolQueryBuilder getBoolQueryBuilder(BoolQueryBuilder boolQuery, SearchParams params) {
        switch (SearchCondition.valueOf(params.getCondition())) {
            case AND:
                boolQuery.must().add(getQueryBuilder(params));
                break;
            case NOT:
                boolQuery.mustNot().add(getQueryBuilder(params));
                break;
            case FILTER:
                boolQuery.filter().add(getQueryBuilder(params));
                break;
            case OR:
            default:
                boolQuery.should().add(getQueryBuilder(params));
                break;
        }
        
        return boolQuery;
    }
    
    private QueryBuilder getQueryBuilder(SearchParams params) {
        if (params.getSearchType() == null) {
            return getMatchPhrasePrefixQueryBuilder(params.getSearchParams()[0], params.getSearchParams()[1]);
        }
        switch (params.getSearchType()) {
            case MULTI_MATCH:
                return getMultiMatchQueryBuilder(params.getSearchParams()[0], Arrays.copyOfRange(params.getSearchParams(), 1, params.getSearchParams().length - 1));
            case EXACT_MATCH:
                return getTemQueryBuilder(params.getSearchParams()[0], params.getSearchParams()[1]);
            case EXIST_MATCH:
                return getExistFieldQueryBuilder(params.getSearchParams()[0]);
            // TODO this is not a very good implementation since we have to convert back and forth 
            // between String (because of my stupid design at first, all parameters are string!), Date (this old Java library is being used currently) 
            // and Joda API (ElasticSearch uses Joda and the modern preferred way to do with date manipulation)
            // Also we can investigate on the document_mapping.json, justify the format of date field => decrease the conversion complex
            // Also this RANGE_MATCH can be used for number type: try to convert to Date, if null -> it's a number range query
            case RANGE_MATCH: 
                final String fromDate = DateFormatter.fromDateToJoda(params.getSearchParams()[1]);
                final String toDate = DateFormatter.fromDateToJoda(params.getSearchParams()[2]);
                final String fromValue = fromDate != null ? fromDate : params.getSearchParams()[1];
                final String toValue = toDate != null ? toDate : params.getSearchParams()[2];
                return getRangeQueryBuilder(params.getSearchParams()[0], fromValue, toValue);
            case PREFIX_MATCH:
            default:
                return getMatchPhrasePrefixQueryBuilder(params.getSearchParams()[0], params.getSearchParams()[1]);
        }
    }
    
    private <T> MultiMatchQueryBuilder getMultiMatchQueryBuilder(T searchText, String... searchFields) {
        return QueryBuilders.multiMatchQuery(searchText, searchFields);
    }
    
    private <T> MatchPhrasePrefixQueryBuilder getMatchPhrasePrefixQueryBuilder(String searchField, T searchText) {
        return QueryBuilders.matchPhrasePrefixQuery(searchField, searchText);
    }
    
    // why error? match_phrase not supports zero_term_query? -> switch to term query instead
    private <T> MatchPhraseQueryBuilder getMatchPhraseQueryBuilder(String searchField, T searchValue) {
        return QueryBuilders.matchPhraseQuery(searchField, searchValue);
    }
    
    private <T> TermQueryBuilder getTemQueryBuilder(String searchField, T searchValue) {
        return QueryBuilders.termQuery(searchField, searchValue);
    }
    
    private <T> RangeQueryBuilder getRangeQueryBuilder(String searchField, T fromValue, T toValue) {
        return QueryBuilders.rangeQuery(searchField).gte(fromValue).lte(toValue);
    }
    
    private ExistsQueryBuilder getExistFieldQueryBuilder(String searchField) {
        return QueryBuilders.existsQuery(searchField);
    }

    private List<E> extractResultFromSearchResponse(SearchResponse response) {
        List<E> entityList = new ArrayList<>();
        for (SearchHit searchHit : response.getHits().getHits()) {
            entityList.add(GSON_MAPPER.fromJson(searchHit.getSourceAsString(), entityType));
        }
        return entityList;
    }
    
    @Override
    public SearchResultData<E> searchWithRecordCount(final SearchQueryData queryData) {
        SearchResultData<E> searchResultData = new SearchResultData<>();
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(buildSearchRequest(queryData));
        }
        catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to search. Exception: " + e.getMessage());
        }
        
        searchResultData.setNumberOfRecords(searchResponse.getHits().getTotalHits());
        searchResultData.setResultList(extractResultFromSearchResponse(searchResponse));
        return searchResultData;
    }
    
    @Override
    public void saveOrUpdate(final E entity, final String id) {
        saveOrUpdate(entity, id, ServiceMode.SYNC);
    }
    
    @Override
    public void saveOrUpdate(final E entity, final String id, final ServiceMode mode) {
        if (existDocument(indexName, documentType, id)) {
            delete(entity, id, ServiceMode.SYNC);
        }
        
        IndexRequest indexRequest = buildIndexRequest(entity, id);
        if (ServiceMode.isSync(mode)) {
            index(indexRequest);
            return;
        }

        indexAsync(indexRequest);
    }
    
    private void index(IndexRequest indexRequest) {
        try {
            client.index(indexRequest);
        }
        catch (ElasticsearchException e) {
            commonElasticSearchException(e);
        }
        catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to index the entity document. Exception: " + e.getMessage());
        }
    }
    
    private void indexAsync(IndexRequest indexRequest) {
        client.indexAsync(indexRequest, buildAsyncListener());
    }
    
    private <T> ActionListener<T> buildAsyncListener() {
        return new ActionListener<T>() {
            @Override
            public void onResponse(T response) {
                LOGGER.info("Asynchronous operation has been done successfully");
            }
            @Override
            public void onFailure(Exception e) {
                throw new ElasticSearchServiceException("Unable to operate asynchronous action. Exception: " + e.getMessage());
            }
        };
    }
    
    @Override
    public Map<String, Map<String, Object>> createMappingInfo(final Map<String, String> fieldsToIndex) {
        Map<String, Map<String, Object>> mappings = new HashMap<>();
        for (PropertyDescriptor propertyDesc : getBeanInfo(entityType).getPropertyDescriptors()) {
            Map<String, Object> fieldMapppingInfo = new HashMap<>();
            String fieldType = StringUtils.isEmpty(fieldsToIndex.get(propertyDesc.getName())) ? FieldType.TEXT.getType() 
                    : fieldsToIndex.get(propertyDesc.getName());
            fieldMapppingInfo.put(FIELD_TYPE, fieldType);
            if (FieldType.DATE.getType().equals(fieldType)) {
                fieldMapppingInfo.put(FIELD_DATE_FORMAT, DATE_FORMAT);
            }
                
            fieldMapppingInfo.put(FIELD_INDEX, false);
            if (fieldsToIndex.containsKey(propertyDesc.getName())) {
                fieldMapppingInfo.put(FIELD_INDEX, true);
            }
            mappings.put(propertyDesc.getName(), fieldMapppingInfo);
        }
        return mappings;
    }
        
    @Override
    public Map<String, Map<String, Object>> createMappingInfo(final List<String> fieldsToIndex) {
        final Map<String, String> fieldsToIndexMap = new HashMap<>();
        for (String field : fieldsToIndex) {
            fieldsToIndexMap.put(field, FieldType.TEXT.getType());
            if (FIELD_TIMESTAMP.equals(field)) {
                fieldsToIndexMap.put(field, FieldType.LONG.getType());
            }
        }
        
    	return createMappingInfo(fieldsToIndexMap);
    }
     
    @Override
    public void createIndexAlias(final String indexName, final String alias) {
        HttpEntity entity = new NStringEntity(buildJsonAliasRequest(AliasRequestType.ADD, indexName, alias), ContentType.APPLICATION_JSON);
        try {
            getLowLevelClient().performRequest(AliasRequestType.HttpMethod.POST.getMethod(), AliasRequestType.AliasActions.ALIAS_ENDPOINT, new HashMap<>(), entity);
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to create alias for the index. Exception: " + e.getMessage());
        }
    }
    
    @Override
    public void removeIndexAlias(final String indexName, final String alias) {
        HttpEntity entity = new NStringEntity(buildJsonAliasRequest(AliasRequestType.REMOVE, indexName, alias), ContentType.APPLICATION_JSON);
        try {
            getLowLevelClient().performRequest(AliasRequestType.HttpMethod.POST.getMethod(), AliasRequestType.AliasActions.ALIAS_ENDPOINT, new HashMap<>(), entity);
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to remove alias for the index. Exception: " + e.getMessage());
        }
    }
    
    private String buildJsonAliasRequest(AliasRequestType type, String index, String alias) {
        String jsonAliasRequest = type.getJsonRequest();
        jsonAliasRequest = jsonAliasRequest.replace("sampleIndex", index);
        jsonAliasRequest = jsonAliasRequest.replace("sampleAlias", alias);
        return jsonAliasRequest;
    }
    
    @Override
    public void reIndex(final String sourceIndex, final String destinationIndex) {
        // TODO
    }
    
    @Override
    public void refresh(String... indexList) {
        RefreshRequest request = new RefreshRequest(indexList); 
        try {
            client.indices().refresh(request);
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to refresh the index. Exception: " + e.getMessage());
        }
    }
    
    @Override
    public void flush(String... indexList) {
        FlushRequest request = new FlushRequest(indexList);
        try {
            client.indices().flush(request);
        } catch (IOException e) {
            throw new ElasticSearchServiceException("Unable to flush the index. Exception: " + e.getMessage());
        }
    }
    
    @Override
    public void setRefreshPolicy(RefreshPolicy policy) {
        REFRESH_POLICY = policy;
    }

}
