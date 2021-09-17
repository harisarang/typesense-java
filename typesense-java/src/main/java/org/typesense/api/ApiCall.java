package org.typesense.api;

import java.time.Duration;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.typesense.model.ErrorResponse;
import org.typesense.interceptor.LoggingInterceptor;
import org.typesense.api.exceptions.*;
import org.typesense.resources.Node;
import org.typesense.resources.RequestHandler;



import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;


public class ApiCall {

    private final Configuration configuration;
    private final List<Node> nodes;

    private static int nodeIndex=0;

    private static final String API_KEY_HEADER = "X-TYPESENSE-API-KEY";
    private static final Logger logger = LoggerFactory.getLogger(ApiCall.class);
    private final Client client;
    private final String apiKey;
    private final Duration retryInterval;

    public ApiCall(Configuration configuration) {
        this.configuration = configuration;
        this.nodes = configuration.nodes;
        this.apiKey = configuration.apiKey;
        this.retryInterval = configuration.retryInterval;

        ClientConfig clientConfig = new ClientConfig();
        if (logger.isTraceEnabled()) {
            clientConfig = clientConfig.register(new LoggingInterceptor());
        }
        // TODO: SET_METHOD_WORKAROUND Generates an illegal reflective access operation for the patch op
        this.client = ClientBuilder.newClient(clientConfig)
                .register(JacksonFeature.class)
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
    }

    boolean isDueForHealthCheck(Node node){
        return Duration.between(node.lastAccessTimestamp, LocalDateTime.now()).getSeconds() > configuration.healthCheckInterval.getSeconds();
    }

    //Loops in a round-robin fashion to check for a healthy node and returns it
    Node getNode(){
        if(configuration.nearestNode!=null){
            if(isDueForHealthCheck((configuration.nearestNode) ) || configuration.nearestNode.isHealthy){
                return configuration.nearestNode;
            }
        }

        int i=0;
        Node testNode;

        while(i < configuration.nodes.size()){
            testNode = configuration.nodes.get(nodeIndex);

            if(testNode.isHealthy || isDueForHealthCheck((testNode))){
                return testNode;
            }

            i += 1;
            nodeIndex = (nodeIndex+1)%configuration.nodes.size();
        }

        return configuration.nodes.get(nodeIndex);
    }

    void setNodeHealthStatus(Node node, boolean status){
        node.isHealthy = status;
        node.lastAccessTimestamp = LocalDateTime.now();
    }

    private TypesenseError getException(Response response){
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        String message = errorResponse.message;
        int status_code = response.getStatus();

        if(status_code == 400)
            return new RequestMalformed(message,status_code);
        else if(status_code == 401)
            return new RequestUnauthorized(message,status_code);
        else if(status_code == 404)
            return new ObjectNotFound(message,status_code);
        else if(status_code == 409)
            return new ObjectAlreadyExists(message,status_code);
        else if(status_code == 422)
            return new ObjectUnprocessable(message,status_code);
        else if(status_code >=500 && status_code <= 599)
            return new ServerError(message,status_code);
        else
            return new HttpError(message,status_code);
    }


    <T, Q> T get(String endpoint,Q queryParameters, Class<T> resourceClass){

        /**
         * Lambda function which implements the RequestHandler interface
         * which is passed as a parameter to makeRequest function
         * and returns T as the response entity.
         * This is similar for all type of requests.
         */
        RequestHandler r =  (String REST_URI) -> populateQueryParameters2(this.client.target(REST_URI), queryParameters)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .get();

        return makeRequest(endpoint,r,resourceClass);
    }

    <T> T get(String endpoint, Class<T> resourceClass){
        RequestHandler r =  (String REST_URI) -> this.client.target(REST_URI)
                 .request(MediaType.APPLICATION_JSON)
                 .header(API_KEY_HEADER,apiKey)
                 .get();

         return makeRequest(endpoint,r,resourceClass);
    }

    <T> T get(String endpoint){
        RequestHandler r =  (String REST_URI) -> this.client.target(REST_URI)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(API_KEY_HEADER,apiKey)
                .get();

        return makeRequest(endpoint,r,null);
    }

    <T,R> T put(String endpoint, R body, Class<T> resourceClass){

        RequestHandler r =  (String REST_URI) -> this.client.target(REST_URI)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .put(Entity.json(body));

        return makeRequest(endpoint,r,resourceClass);
    }


    <T, R> T post(String endpoint, R body, Class<T> resourceClass){

        RequestHandler r =  (String REST_URI) -> this.client.target(REST_URI)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .post(Entity.json(body));

        return makeRequest(endpoint,r,resourceClass);
    }

    <T> T post(String endpoint, T body){

        RequestHandler r =  (String REST_URI) -> this.client.target(REST_URI)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .post(Entity.json(body));

        return makeRequest(endpoint,r,null);
    }


    <T,R,Q> T post(String endpoint, R body, Q queryParameters, Class<T> resourceClass){

        RequestHandler r =  (String REST_URI) -> populateQueryParameters2(this.client.target(REST_URI), queryParameters)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .post(Entity.json(body));

        return makeRequest(endpoint,r,resourceClass);
    }

    <T> T post(String endpoint, HashMap<String, String> queryParameters){

        RequestHandler r =  (String REST_URI) -> populateQueryParameters(this.client.target(REST_URI), queryParameters)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .post(Entity.json(null));

        return makeRequest(endpoint,r,null);
    }

    <T> T post(String endpoint, HashMap<String , List<HashMap<String,String>>> body, HashMap<String, String> queryParameters, Class<T> resourceClass){

        RequestHandler r =  (String REST_URI) -> populateQueryParameters(this.client.target(REST_URI), queryParameters)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .post(Entity.json(body));

        return makeRequest(endpoint,r,resourceClass);
    }

    <T> T post(String endpoint){

        RequestHandler r =  (String REST_URI) -> this.client.target(REST_URI)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .post(Entity.json(null));

        return makeRequest(endpoint,r,null);
    }

    <T, Q> T delete(String endpoint, Q queryParameters){
        RequestHandler r =  (String REST_URI) -> populateQueryParameters2(this.client.target(REST_URI),queryParameters)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .delete();

        return makeRequest(endpoint,r,null);
    }


    <T> T delete(String endpoint,  Class<T> resourceClass){
        RequestHandler r =  (String REST_URI) -> this.client.target(REST_URI)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .delete();

        return makeRequest(endpoint,r, resourceClass);
    }

    <T> T delete(String endpoint){
        RequestHandler r =  (String REST_URI) -> this.client.target(REST_URI)
                .request(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER,apiKey)
                .delete();

        return makeRequest(endpoint,r,null);
    }

    /**
     * Actual function which makes the http request
     * @param endpoint particular endpoint of the resource
     * @param requestHandler lambda function interface for the particular request
     * @param <T> response entity
     * @return http response
     */

    <T> T makeRequest(String endpoint, RequestHandler requestHandler, Class<T> resourceClass){
        int num_tries = 0;
        Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Response response;

        while(num_tries < this.configuration.numRetries){
            num_tries += 1;

            Node node = this.getNode();

            String URI = node.baseUrl;

            try{
                String url = URI + endpoint;
                response =  requestHandler.handleRequest(url);

                if(response.getStatus()<500){
                    this.setNodeHealthStatus(node,true);
                }

                if(response.getStatus()>=200 && response.getStatus()<300){
                    return handleResponse(response,resourceClass);
                }

                if(response.getStatus()>=400){
                    throw getException(response);
                }
            }
            catch(ServerError|HttpError e){
                this.setNodeHealthStatus(node,false);
                logger.trace("Request to " + node.host + " failed because: " + e.message);
                logger.trace("Sleeping for "+ this.retryInterval.getSeconds() + "s and then retrying request");
                try {
                    Thread.sleep(this.retryInterval.getSeconds());
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            } catch (TypesenseError typesenseError) {
                typesenseError.printStackTrace();
                return null;
            }
        }
        return  null;
    }


    /**
     * Adds query parameters to the http request
     * @param client WebTarget object pointing to the required endpoint
     * @param queryParameters Map of the query parameters
     * @return WebTarget with the query parameters added
     */

    private WebTarget populateQueryParameters(WebTarget client, HashMap<String, String> queryParameters) {
        if(queryParameters!=null){
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                client = client.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return client;
    }

    private <T> WebTarget populateQueryParameters2(WebTarget client, T queryParameters) {
        if(queryParameters!=null){
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> map = objectMapper.convertValue(queryParameters, Map.class);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                StringBuilder value = new StringBuilder();
                if(entry.getValue() instanceof ArrayList){
                    for(int i=0;i<((ArrayList<?>) entry.getValue()).size();i++){
                        value.append(((ArrayList<?>) entry.getValue()).get(i));
                        if(i != ((ArrayList<?>) entry.getValue()).size()-1)
                            value.append(",");
                    }
                    client = client.queryParam(entry.getKey(), value);
                }
                else {
                    client = client.queryParam(entry.getKey(), entry.getValue());
                }
            }
        }
        return client;
    }

    /**
     * Function to create a map from the json response
     * @param response Jersey Response object
     * @param <T>
     * @return HashMap containing the response
     */

    <T> T handleResponse(Response response, Class<T> resourceClass){

        if(resourceClass == null){
            ObjectMapper mapper = new ObjectMapper();
            String json = response.readEntity(String.class);

            try {
                HashMap<String, Object> map = mapper.readValue(json, HashMap.class);
                return (T)map;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        else{
            return response.readEntity(resourceClass);
        }
        return null;
    }

}


