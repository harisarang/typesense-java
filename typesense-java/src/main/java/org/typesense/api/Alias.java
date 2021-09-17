package org.typesense.api;

import org.typesense.model.CollectionAlias;

public class Alias {

    public ApiCall apiCall;
    public String name;

    public Alias(ApiCall apiCall, String name) {
        this.apiCall = apiCall;
        this.name = name;
    }

    public CollectionAlias retrieve(){
        return this.apiCall.get(this.getEndpoint(),CollectionAlias.class);
    }

    public CollectionAlias delete(){
        return this.apiCall.delete(this.getEndpoint(),CollectionAlias.class);
    }

    public String getEndpoint(){
        return Aliases.RESOURCE_PATH + "/" + this.name;
    }
}
