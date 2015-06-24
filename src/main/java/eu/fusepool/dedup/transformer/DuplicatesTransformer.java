/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.dedup.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fuberlin.wiwiss.silk.Silk;
import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.RdfGeneratingTransformer;

public class DuplicatesTransformer extends RdfGeneratingTransformer {

    final String BASE_URI = "http://example.org/";
    final String TURTLE_MIME_TYPE = "text/turtle";
    final String RDF_MIME_TYPE = "application/rdf+xml";
    boolean asynchronous = false;

    private static final Logger log = LoggerFactory.getLogger(DuplicatesTransformer.class);
    
    public DuplicatesTransformer() {
    }

    public DuplicatesTransformer(boolean asynchronous) {
    	this.asynchronous = asynchronous;
    	log.info("Asynchronous: " + isLongRunning());
    }

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        try {
              Set<MimeType> mimeSet = new HashSet<MimeType>();
              mimeSet.add(new MimeType(TURTLE_MIME_TYPE + ";charset=UTF-8"));
              mimeSet.add(new MimeType(RDF_MIME_TYPE + ";charset=UTF-8"));
              return Collections.unmodifiableSet(mimeSet);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
    	String rdfDataFormat = SupportedFormat.TURTLE;//entity.getType().getBaseType();
    	//String requestUri = entity.getRequest().getRequestURI();    	
    	InputStream configIn = null;
    	String queryString = entity.getRequest().getQueryString();
    	log.info("Query string: " + queryString);
    	//String configUri = getRequestParamValue(queryString, "config");	
    	String configUri = entity.getRequest().getParameter("config");    	
    	log.info("Config file URI: " + configUri);
    	//log.info("Async: " + entity.getRequest().getParameter("asynchronous"));
    	if(configUri != null) {    	  
    	  configIn = getRemoteConfigFile(configUri);
    	}
    	
        final InputStream inputRdfData = entity.getData();
        TripleCollection duplicates = findSameEntities(inputRdfData, rdfDataFormat, configIn);
        return duplicates;
    }
    
    protected InputStream getRemoteConfigFile(String configName) throws IOException{
    	URL configUrl = new URL(configName);
    	URLConnection connection = configUrl.openConnection();
    	return connection.getInputStream();
    }

    /**
     * The client RDF data is always used as the source data source, of type file, for the comparisons with a target data source. 
     * The target data source can be of type file or a SPARQL endpoint. If the target data source in the Silk config file
     * is set to be of type file then the same client data will be used and the task is a deduplication task (Silk works only with local files). 
     * The updated configuration file and the input RDF data and the output files are stored in the /tmp/ folder.
     * @param inputRdf
     * @return
     * @throws IOException
     */
    protected TripleCollection findSameEntities(InputStream inputRdf, String rdfFormat, InputStream configIn) throws IOException {    	
    	// Default silk config file
    	File configFile = null;
    	if(configIn != null){
    		configFile = FileUtil.inputStreamToFile(configIn, "silk-config-", ".xml");
    	}
    	else {
    		configFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("silk-config-file.xml"), "silk-config-", ".xml");
    	}
    	
        // file with original data serialized in N-TRIPLE format
        File ntFile = File.createTempFile("input-rdf", ".nt");
        // file containing the equivalences
        File outFile = File.createTempFile("output-", ".nt");
        
        // update the config file with the paths of the source datasource and output files and the format
        // if the type of target datasource is "file" update the path (deduplication) 
        SilkConfigFileParser silkParser = new SilkConfigFileParser(configFile.getAbsolutePath());
        silkParser.updateOutputFile(outFile.getAbsolutePath());
        silkParser.updateSourceDataSourceFile(ntFile.getAbsolutePath(), "N-TRIPLE");
        if (silkParser.getTargetDataSourcetype().equals("file")) {
            silkParser.updateTargetDataSourceFile(ntFile.getAbsolutePath(), "N-TRIPLE"); //deduplication
        }
        silkParser.saveChanges();
        
        // change the format into N-TRIPLE
        Parser parser = Parser.getInstance();
        TripleCollection origGraph =  parser.parse(inputRdf, rdfFormat);
        Serializer serializer = Serializer.getInstance();
        serializer.serialize(new FileOutputStream(ntFile), origGraph, SupportedFormat.N_TRIPLE);

        // interlink entities
        Silk.executeFile(configFile, null, 1, true);
        log.info("Interlinking task completed."); 
        TripleCollection equivalences = parseResult(outFile); 
        
        // add the equivalence set to the input rdf data to be sent back to the client
        TripleCollection resultGraph = new SimpleMGraph();
        resultGraph.addAll(origGraph);
        resultGraph.addAll(equivalences);
        
        // remove all temporary files
        configFile.delete();        
        ntFile.delete();
        outFile.delete();

        // returns the result to the client
        return resultGraph;
    }


    /**
     * Reads the silk output (n-triples) and returns the owl:sameas statements
     * as a result
     *
     * @param file
     * @return
     * @throws IOException
     */
    public TripleCollection parseResult(File file) throws IOException {
        Parser parser = Parser.getInstance();
        return parser.parse(new FileInputStream(file), SupportedFormat.N_TRIPLE);
    }
    
    @Override
    public boolean isLongRunning() {        
        return this.asynchronous;
    }
    
}
