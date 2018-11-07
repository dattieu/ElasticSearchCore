
package com.wse.common.elasticsearch.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("rawtypes")
@Configuration
public class ElasticSearchConfig extends AbstractFactoryBean {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchConfig.class);

    @Value("${elasticsearch.cluster-name}")
    private String clusterName;
    
    @Value("${elasticsearch.cluster-nodes}")
    private String clusterNodes;
    
    @Value("${elasticsearch.protocol}")
    private String protocol;
    
    @Value("${elasticsearch.domain}")
    private String domain;
    
    @Value("${elasticsearch.port}")
    private int port;
    
    @Value("${elasticsearch.alternate-port}")
    private int altPort;
    
    private RestHighLevelClient restHighLevelClient;
    
    @Override
    public void destroy() {
        try {
            if (restHighLevelClient != null) {
                restHighLevelClient.close();
            }
        } catch (final Exception e) {
            LOGGER.error("Error closing ElasticSearch client: ", e);
        }
    }

    @Override
    public Class<RestHighLevelClient> getObjectType() {
        return RestHighLevelClient.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public RestHighLevelClient createInstance() {
        return buildClient();
    }

    private RestHighLevelClient buildClient() {
        try {
            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(domain, port, protocol),
                            new HttpHost("domain", altPort, protocol)));
        } catch (Exception e) {
            LOGGER.error("Error building rest client: ", e);
        }
        return restHighLevelClient;
    }
    
}
