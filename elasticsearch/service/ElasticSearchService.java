package com.wse.common.elasticsearch.service;

import java.util.List;
import java.util.Map;

import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;

import com.google.gson.JsonObject;
import com.wse.common.elasticsearch.exception.ElasticSearchServiceException;

/**
 * @author dat.tt2
 *
 * @param <E>
 */
public interface ElasticSearchService <E> {
    
    /**
     * The ASYNC mode methods do not block and returns immediately.
     *
     */
    enum ServiceMode {
        SYNC, ASYNC;
        
        public static boolean isAsync(ServiceMode mode) {
            return ASYNC.equals(mode);
        }
        
        public static boolean isSync(ServiceMode mode) {
            return SYNC.equals(mode);
        }
    }
    
    /**
     * The type of the Elastic Search request
     *
     */
    enum RequestType {
        INDEX, UPDATE, DELETE
    }
    
    /**
     * <pre>
     * Delete index (if exists), create a new one and then make bulk indexing
     * By default, this uses asynchronous operations
     * </pre>
     * @param indexName         the name of the index
     * @param documentType      the document type of the index
     * @param mappings          the field mappings for the index (either in Map format or JSON format)
     * @param entityMapWithId   a map contains the documents with keys are entity IDs
     */
    void initData(final String indexName, final String documentType, final Object mappings, final Map<String, E> entityMapWithId);
    
    /**
     * <pre>
     * Configure index settings with a specific type. Usually one index has only one type.
     * By default, calling this function first before other document manipulation related functions
     * </pre>
     * @param indexName     the name of the index that will be used for all later actions
     * @param documentType  the document type for this index.
     */
    void configureIndex(final String indexName, final String documentType);
    
    /**
     * <pre>
     * Check if an index exists or not
     * </pre>
     * @param indexName     the name of the index
     * @param documentType  the document type of the index
     * @return true         if the index exists
     */
    boolean existIndex(final String indexName, final String documentType);
    
    /**
     * <pre>
     * Check if the index currently in use exists or not
     * </pre>
     * @return true if the index exists
     */
    boolean existIndex();
    
    /**
     * <pre>
     * Check if a document exists
     * </pre>
     * @param documentName     the name of the document
     * @param documentType     the type of the document
     * @param documentId       the id of the document
     * @return
     */
    boolean existDocument(final String documentName, final String documentType, final String documentId);
    
    /**
     * <pre>
     * Create a new index with mappings
     * By default, for efficient use, each index should contain only documents of one type
     * Recommend using this over {@link #createIndex(String)}
     * </pre>
     * @param indexName     the name of the index
     * @param documentType  the document type
     * @param mappings      mappings for the index with the document type (either in Map format or JSON format)
     */
    void createIndexWithMappings(final String indexName, final String documentType, final Object mappings);

    /**
     * <pre>
     * Create a new index (no mappings and no specific document type)
     * Recommend using {@link #createIndexWithMappings(String, String, Object)}
     * </pre>
     * @param indexName the name of the index
     */
    @Deprecated
    void createIndex(final String indexName);
    
    /**
     * <pre>
     * Create mappings for an existing index
     * </pre>
     * @param indexName     the name of the index
     * @param documentType  the document type
     * @param mappings      mappings for the index
     */
    void createIndexMapping(final String indexName, final String documentType, final Object mappings);
    
    /**
     * <pre>
     * Create settings for an existing index
     * </pre>
     * @param indexName     the name of the index
     * @param settings      settings for the index in string format (TODO: create settings for non-string format)
     */
    void createIndexSetting(final String indexName, final String settings);
    
    /**
     * <pre>
     * Delete an existing index
     * </pre>
     * @param indexName the name of the index
     */
    void deleteIndex(final String indexName);

    /**
     * <pre>
     * Index/Create a new document. @See AccountDocument
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * Recommend using {@link #saveOrUpdate(E, String, ServiceMode)} or {@link #upsert(E, String, ServiceMode)}
     * </pre>
     * @param entity    a document of an entity
     * @param id        the id of the entity document
     * @exception       ElasticSearchServiceException
     */
    @Deprecated
    void index(final E entity, final String id);
    
    
    /**
     * <pre>
     * Index/Create a large collection of documents
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * Recommend using {@link #bulkIndex(E, ServiceMode)}
     * </pre>
     * @param entityMapWithId   a map contains the documents with keys are entity IDs
     * @exception  ElasticSearchServiceException
     */
    @Deprecated
    void bulkIndex(final Map<String, E> entityMapWithId);
    
    /**
     * <pre>
     * Index/Create a large collection of documents
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * Recommend using this over {@link #bulkIndex(Map)}
     * See also {@link #bulk(Map, Map, Map, ServiceMode)} for generic use
     * </pre>
     * @param entityMapWithId   a map contains the documents with keys are entity IDs
     * @param mode              the desired service mode. @See ServiceMode
     */
    void bulkIndex(final Map<String, E> entityMapWithId, final ServiceMode mode);
    
    /**
     * <pre>
     * Index (actually {@link #upsert(E, String, ServiceMode)}) and/or delete large sets of documents
     * Use this over {@link #bulkIndex(Map, ServiceMode)} for generic operations
     * </pre>
     * @param entityMapToIndex  a map contains the documents with keys are entity IDs, to be indexed
     * @param entityMapToDelete a map contains the documents with keys are entity IDs, to be deleted
     * @param entityMapToUpdate a map contains the documents with keys are entity IDs, to be upserted (See {@link #upsert(E, String, ServiceMode)})
     * @param mode              the desired service mode. @See ServiceMode
     */
    void bulk(final Map<String, E> entityMapToIndex, final Map<String, E> entityMapToDelete, final Map<String, E> entityMapToUpdate, 
            final ServiceMode mode);
    
    /**
     * <pre>
     * Update an existing document
     * Recommend using {@link #saveOrUpdate(E, String, ServiceMode)} or {@link #upsert(E, String, ServiceMode)}
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * </pre>
     * @param entity    the updating document
     * @param id        the id of the updating document
     * @exception       ElasticSearchServiceException
     */
    @Deprecated
    void update(final E entity, final String id);
    
    /**
     * <pre>
     * Update an existing document
     * If the document does not exist, create a new one and index it
     * This is somehow similar to {@link #saveOrUpdate(E, String, ServiceMode)}
     * </pre>
     * @param entity    the updating document
     * @param id        the id of the updating document
     * @param mode      the desired service mode. @See ServiceMode
     */
    void upsert(final E entity, final String id, final ServiceMode mode);
    
    /**
     * <pre>
     * Delete an existing document
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * Recommend using {@link #delete(E, String, ServiceMode)}
     * </pre>
     * @param entity    the deleting document
     * @param id        the id of the deleting document
     * @exception       ElasticSearchServiceException
     */
    @Deprecated
    void delete(final E entity, final String id);
    
    /**
     * <pre>
     * Delete an existing document
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * Recommend using this over {@link #delete(E, String)}
     * </pre>
     * @param entity    a document of the entity
     * @param id        the id of the entity document
     * @param mode      the desired service mode. @See ServiceMode
     */
    void delete(final E entity, final String id, final ServiceMode mode);
    
    /**
     * <pre>
     * Count all the documents that match the search query
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * </pre>
     * @return      the number of total documents
     * @exception   ElasticSearchServiceException
     */
    long count(final SearchQueryData queryData);
    
    /**
     * <pre>
     * Count the number of results match against the request
     * @param request
     * @return
     */
    long count(final JsonObject request);
    
    /**
     * <pre>
     * Search all
     * Not recommend using this because of bad performance
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * </pre>
     * @return      all the documents
     * @exception   ElasticSearchServiceException
     */
    @Deprecated
    List<E> search();
    
    /**
     * <pre>
     * Search against one field only
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * </pre>
     * @param fieldToSearch the field to apply the search against
     * @param textToSearch  the text to search
     * @return              all the documents that match the search criteria
     * @exception           ElasticSearchServiceException
     */
    List<E> search(final String fieldToSearch, final String textToSearch);
    
    /**
     * <pre>
     * Multiple field matching search
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * Can only search against indexed fields
     * @param queryData     the search query. Example:
     * {
          "searchParams" : [ {
            "searchParams" : [ "username", "dat" ],
            "condition" : "OR"
          }, {
            "searchParams" : [ "email", "son" ],
            "condition" : "AND"
          } ],
          "from" : 0,
          "size" : 10
        }
     * </pre>
     * @return              all the documents that match the search criteria
     * @exception  ElasticSearchServiceException
     */
    List<E> search(final SearchQueryData queryData);
    
    /**
     * <pre>
     * Multiple field match search and also return the number of records (@See SearchResultData)
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * </pre>
     * @param queryData the search query    (See {@link #search(SearchQueryData)})
     * @return          all the documents that match the search criteria
     * @exception       ElasticSearchServiceException
     */
    SearchResultData<E> searchWithRecordCount(final SearchQueryData queryData);
    
    /**
     * <pre>
     * Delete the entity if exists, then index a new one
     * Recommend using {@link #saveOrUpdate(E, String, ServiceMode)} or {@link #upsert(E, String, ServiceMode)}
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * </pre>
     * @param entity    a document of the entity
     * @param id        the id of the entity document
     */
    @Deprecated
    void saveOrUpdate(final E entity, final String id);
    
    /**
     * <pre>
     * Recommend using this over {@link #saveOrUpdate(E, String)}
     * Basically, delete the entity if exists, then index a new one
     * See also {@link #upsert(E, String, ServiceMode)} which has similar use
     * This function gets default indexName and documentType from {@link #configureIndex(String, String)}
     * </pre>
     * @param entity    a document of the entity
     * @param id        the id of the entity document
     * @param mode      the desired service mode. @See ServiceMode
     */
    void saveOrUpdate(final E entity, final String id, final ServiceMode mode);
    
    /**
     * <pre>
     * Create a mapping file for the current document entity, specifying which field would be indexed
     * </pre>
     * @param   fieldsToIndex	a list of field names to be indexed
     * @return  a map containing index mapping information
     */
    Map<String, Map<String, Object>> createMappingInfo(final List<String> fieldsToIndex);
    
    /**
     * <pre>
     * Create a mapping file for the current document entity, specifying which field would be indexed
     * and which type should each indexed field have
     * </pre>
     * @param   fieldsToIndex a map of field names to be indexed and corresponding type
     * @return  a map containing index mapping information
     */
    Map<String, Map<String, Object>> createMappingInfo(final Map<String, String> fieldsToIndex);
    
    /**
     * <pre>
     * Create an alias for an existing index
     * Use this for re-indexing situations
     * </pre>
     * @param indexName the name of the index
     * @param alias     the desired alias name for the index
     */
    void createIndexAlias(final String indexName, final String alias);
    
    /**
     * <pre>
     * Remove the alias for an existing index
     * Use this for re-indexing situations
     * </pre>
     * @param indexName the name of the index
     * @param alias     the desired alias name for the index
     */
    void removeIndexAlias(final String indexName, final String alias);
    
    /**
     * <pre>
     * Copy all documents from an index to another index (with new settings, structure, ...)
     * Usually used when new configuration, settings or document structure is changed
     * Change alias when move to another index. See {@link #createIndexAlias(String, String)}, {@link #removeIndexAlias(String, String)}
     * </pre>
     * @param sourceIndex       the old index
     * @param destinationIndex  the new index
     */
    void reIndex(final String sourceIndex, final String destinationIndex);
    
    /**
     * <pre>
     * Refresh an existing index or many indices
     * Specially used after just indexing a new document to make it search-able
     * </pre>
     * @param indexList     a list of indices
     */
    void refresh(String... indexList);
    
    /**
     * <pre>
     * Flush forces the engine to commit the new indexed indices (evict the cache)
     * Much more expensive than {@link #refresh(String...)}
     * </pre>
     * @param indexList     a list of indices
     */
    void flush(String... indexList);
    
    /**
     * <pre>
     * Set the refresh policy for the entire service
     * By default, it should be NONE for better performance
     * </pre>
     * @param policy    the refresh policy
     * See  {@link #org.elasticsearch.action.support.WriteRequest.RefreshPolicy}
     */
    void setRefreshPolicy(RefreshPolicy policy);
    
}
