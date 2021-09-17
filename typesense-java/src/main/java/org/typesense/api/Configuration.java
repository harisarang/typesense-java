package org.typesense.api;

import org.typesense.resources.Node;

import java.time.Duration;
import java.util.List;

public class Configuration {

    public List<Node> nodes;
    public Node nearestNode;
    public Duration connectionTimeout;
    public Duration healthCheckInterval;
    public int numRetries;
    public Duration retryInterval;
    public String apiKey;
    public boolean sendApiKeyAsQueryParam;

    /**
     *
     * @param nodes List of Nodes
     * @param connectionTimeout Duration in seconds
     * @param apiKey String describing the apiKey
     */

    public Configuration(List<Node> nodes, Duration connectionTimeout, String apiKey) {
        this.nodes = nodes;
        this.connectionTimeout = connectionTimeout;
        this.apiKey = apiKey;

        this.healthCheckInterval = Duration.ofSeconds(15);
        this.numRetries =  3;
        this.retryInterval= Duration.ofSeconds(100);
        this.sendApiKeyAsQueryParam = false;
    }

    public Configuration(Node nearestNode, List<Node> nodes, Duration connectionTimeout, String apiKey) {
        this(nodes,connectionTimeout,apiKey);
        this.nearestNode = nearestNode;
    }
}
