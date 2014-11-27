package eu.fusepool.dedup.transformer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import eu.fusepool.dedup.transformer.DuplicatesTransformer;
import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.client.TransformerClientImpl;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.WritingEntity;
import eu.fusepool.p3.transformer.server.TransformerServer;



public class DuplicatesTransformerTest {
	
    final static String CLIENT_DATA_MIME_TYPE = "text/turtle"; //MIME type of the data sent by the client
	final static String TRANSFORMER_MIME_TYPE = "text/turtle"; // MIME type of the transformer output
	private static MimeType transformerMimeType;
    private static MimeType clientDataMimeType;
    static {
        try {
        	transformerMimeType = new MimeType(TRANSFORMER_MIME_TYPE);
        	clientDataMimeType = new MimeType(CLIENT_DATA_MIME_TYPE);
        } catch (MimeTypeParseException ex) {
            Logger.getLogger(DuplicatesTransformerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
	
	// SILK config file used by the mock server
	final String MOCK_SILK = "silk-config-file.xml";
	private static int mockPort = 0;
	private byte[] mockSilkConfig;

	File ttlFile = null;
	File rdfFile = null;
    private String baseUri;
    private byte[] rdfData;
    private byte[] ttlData;
    
    @BeforeClass
	public static void setMockPort() {
		mockPort = findFreePort();
		
	}

    @Before
    public void setUp() throws Exception {
    	
    	// load the SILK config file
    	mockSilkConfig = IOUtils.toByteArray(getClass().getResourceAsStream(MOCK_SILK));
    			
        ttlFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("testfoaf.ttl"), "test-", ".ttl");
        InputStream inttl = new FileInputStream(ttlFile);
        ttlData = IOUtils.toByteArray(inttl);
        inttl.close();
        
        rdfFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("testfoaf.rdf"), "test-", ".rdf");
        InputStream inrdf = new FileInputStream(rdfFile);
        rdfData = IOUtils.toByteArray(inrdf);
        inrdf.close();

        final int port = findFreePort();
        baseUri = "http://localhost:" + port + "/";
        RestAssured.baseURI = "http://localhost:" + port + "/";
        TransformerServer server = new TransformerServer(port);
        server.start(new DuplicatesTransformer());
    }
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(mockPort);  
    
    @After
    public void tearDown() throws Exception {
    	ttlFile.delete();
    	rdfFile.delete();
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
    
    /**
     * The transformer receives data and a url from the client, fetches the xslt from the url, applies the transformation
     * and then check if the transformation is compatible with the expected result.
     * @throws Exception
     */
	@Test
    public void testTransformation() throws Exception {
		
        Transformer t = new TransformerClientImpl(setUpMockServer());
        // the transformer fetches the SILK config file from the mock server, applies its rules and sends the RDF result to the client
        {
            Entity response = t.transform(new WritingEntity() {

                @Override
                public MimeType getType() {
                    return clientDataMimeType;
                }

                @Override
                public void writeData(OutputStream out) throws IOException {
                    out.write(ttlData);
                }
            }, transformerMimeType);

            // the client receives the response from the transformer
            Assert.assertEquals("Wrong media Type of response", transformerMimeType.toString(), response.getType().toString());  
            // Parse the RDF data returned by the transformer after the xslt transformation has been applied to the xml data
            final Graph responseGraph = Parser.getInstance().parse(response.getData(), "text/turtle");
            //checks for the presence of a specific property added by the transformer
            final Iterator<Triple> propertyIter = responseGraph.filter(null, OWL.sameAs, null);
            Assert.assertTrue("No specific property found in response", propertyIter.hasNext());
            
        }
                
	    
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
    
    /**
	 * Set up a service in the mock server to respond to a get request that must be sent by the transformer
	 * on behalf of its client to fetch the SILK config file.
	 * Returns the SILK config file url.
	 */
	private String setUpMockServer() throws UnsupportedEncodingException{
		stubFor(get(urlEqualTo("/config/" + MOCK_SILK))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SC_OK)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(mockSilkConfig)));
	   // prepare the client HTTP POST message with the xml data and the url where to dereference the xslt 
       String xsltUrl = "http://localhost:" + mockPort + "/config/" + MOCK_SILK ;
       // the client sends a request to the transformer with the url of the events data to be fetched
       String queryString = "config=" + URLEncoder.encode(xsltUrl, "UTF-8");
       return RestAssured.baseURI+"?"+queryString;
	}

}
