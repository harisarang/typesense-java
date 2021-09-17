package org.typesense.api;

import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.typesense.model.CollectionResponse;
import org.typesense.model.CollectionSchema;
import org.typesense.model.Field;
import org.typesense.resources.Node;

import java.time.Duration;
import java.util.ArrayList;


public class CollectionsTest extends TestCase {

    public Client client;

    public void setUp() throws Exception {
        super.setUp();

        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(new Node("http","localhost","3001"));

        Configuration configuration = new Configuration(nodes,Duration.ofSeconds(3),"xyz");

        this.client = new Client(configuration);

    }

    @Test
    public void testRetrieveAllCollections() {
        CollectionResponse[] collectionResponses = client.collections().retrieve();
        for(CollectionResponse c:collectionResponses)
            System.out.println(c);
    }

    @Test
    public void testRetrieveSingleCollection(){
        System.out.println(client.collections("Countries").retrieve());
    }

    @Test
    public void testDeleteCollection(){
        System.out.println(client.collections("Countries").delete());
    }

    @Test
    public void testCreateCollection(){

        ArrayList<Field> fields = new ArrayList<>();
        fields.add(new Field().name("countryName").type("string"));
        fields.add(new Field().name("capital").type("string"));
        fields.add(new Field().name("gdp").type("int32").facet(true));

        CollectionSchema collectionSchema = new CollectionSchema();
        collectionSchema.name("Countries").fields(fields).defaultSortingField("gdp");

        CollectionResponse cr = client.collections().create(collectionSchema);
        System.out.println(cr);
    }
}