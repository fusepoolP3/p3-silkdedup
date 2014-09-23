package eu.fusepool.dedup.transformer;



import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
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
	
	final String SILK_CONFIG_FILE = "src/test/resources/silk-config-file.xml";
	final String SOURCE_RDF_FILE = "src/test/resources/testfoaf.ttl";
	final String TARGET_RDF_FILE = "src/test/resources/testfoaf.ttl";
	private String baseUri;
	private byte[] rdfData;
	
	@Before
    public void setUp() throws Exception {
		
		File rdfFile = new File(SOURCE_RDF_FILE);
		InputStream in = new FileInputStream(rdfFile);
        rdfData = IOUtils.toByteArray(in);
        in.close();
        
        final int port = findFreePort();
        baseUri = "http://localhost:"+port+"/";
        RestAssured.baseURI = "http://localhost:"+port+"/";
        TransformerServer server = new TransformerServer(port);
        server.start(new DuplicatesTransformer(SILK_CONFIG_FILE));
    }
	
	
    @Test
    public void turtleOnGet() {
        //Nothing specific here
        Response response = RestAssured.given().header("Accept", "text/turtle")
                .expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
                .get();
    }
    
    
	@Test
	public void testSilk() throws IOException {
		
		Response response = 
        RestAssured.given().header("Accept", "text/turtle")
                .contentType("text/turtle;charset=UTF-8")
                .content(rdfData)
                .expect().statusCode(HttpStatus.SC_OK).content(new StringContains("http://www.w3.org/2002/07/owl#sameAs")).header("Content-Type", "text/turtle").when()
                .post(baseUri);
        
        /*
		Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
        Iterator<Triple> typeTriples = graph.filter(null, OWL.sameAs, null);
        Assert.assertTrue("No equivalent entities found", typeTriples.hasNext());
        */
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
