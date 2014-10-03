package eu.fusepool.dedup.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;

import org.junit.Test;


import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import eu.fusepool.dedup.transformer.DuplicatesTransformer;
import eu.fusepool.p3.transformer.server.TransformerServer;


public class DuplicatesTransformerTest {

    private String baseUri;
    private byte[] rdfData;
    private byte[] ttlData;

    @Before
    public void setUp() throws Exception {
        File ttlFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("testfoaf.ttl"), "test-", ".ttl");
        InputStream inttl = new FileInputStream(ttlFile);
        ttlData = IOUtils.toByteArray(inttl);
        inttl.close();
        
        File rdfFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("testfoaf.rdf"), "test-", ".rdf");
        InputStream inrdf = new FileInputStream(rdfFile);
        rdfData = IOUtils.toByteArray(inrdf);
        inrdf.close();

        final int port = findFreePort();
        baseUri = "http://localhost:" + port + "/";
        RestAssured.baseURI = "http://localhost:" + port + "/";
        TransformerServer server = new TransformerServer(port);
        server.start(new DuplicatesTransformer());
    }
    
    @Test
    public void turtleOnGet() {
    	Response response = RestAssured.given().header("Accept", "text/turtle")
    			.expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
    			.get();
    }

    
    @Test
    public void testSilkRdfTurtle() throws IOException {

        Response response
                = RestAssured.given().header("Accept", "text/turtle")
                .contentType("text/turtle;charset=UTF-8")
                .content(ttlData)
                .expect().statusCode(HttpStatus.SC_OK).content(new StringContains("http://www.w3.org/2002/07/owl#sameAs")).header("Content-Type", SupportedFormat.TURTLE).when()
                .post();

        
         Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
         Iterator<Triple> typeTriples = graph.filter(null, OWL.sameAs, null);
         Assert.assertTrue("No equivalent entities found", typeTriples.hasNext());
         
    }
    
    
    @Test
    public void testSilkRdfXml() throws IOException {

        Response response
                = RestAssured.given().header("Accept", "text/turtle")
                .contentType("application/rdf+xml;charset=UTF-8")
                .content(rdfData)
                .expect().statusCode(HttpStatus.SC_OK).content(new StringContains("sameAs")).header("Content-Type", SupportedFormat.TURTLE).when()
                .post();

        
         Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), SupportedFormat.TURTLE);
         Iterator<Triple> typeTriples = graph.filter(null, OWL.sameAs, null);
         Assert.assertTrue("No equivalent entities found", typeTriples.hasNext());
         
    }
    

    public static int findFreePort() {
        int port = 0;
        try (ServerSocket server = new ServerSocket(0);) {
            port = server.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("unable to find a free port");
        }
        return port;
    }

}
