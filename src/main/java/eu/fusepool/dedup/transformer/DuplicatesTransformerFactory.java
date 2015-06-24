/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.dedup.transformer;

import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author reto
 */
public class DuplicatesTransformerFactory implements TransformerFactory {
	// Parameter (mandatory) used in the POST request to provide the URL of the Silk config file
	public final String SILK_CONFIG_FILE_URI_PARAM = "config";
	// Parameter (optional) used in the POST request to set the transformer execution mode to 
	// asynchronous (true/false). Default value is false (synchronous)
	public final String TRANSFORMER_ASYNC_PARAM = "async";
	
	private static final Logger log = LoggerFactory.getLogger(DuplicatesTransformerFactory.class);
	
	private boolean asynchronous = false; //set the transformer execution mode to asynchronous when set to true
    private final Map<String, Transformer> duplicatesTransformerList = 
            new HashMap<>();
    
    @Override
    public Transformer getTransformer(HttpServletRequest request) {
        //final String silkConfigUri = getRequestParamValue(request.getQueryString(), SILK_CONFIG_FILE_URI_PARAM);
    	final String silkConfigUri = request.getParameter(SILK_CONFIG_FILE_URI_PARAM);
    	final String asyncValue = request.getParameter(TRANSFORMER_ASYNC_PARAM);
    	log.info("factory async value: " + asyncValue);
        //final String asyncValue = getRequestParamValue(request.getQueryString(), TRANSFORMER_ASYNC_PARAM);
        if ( "true".equals(asyncValue) ) {
        	asynchronous = true;
        }
        return getTransfomerFor(silkConfigUri);
    }

    private synchronized Transformer getTransfomerFor(String key) {
        if (duplicatesTransformerList.containsKey( key )) {
            return duplicatesTransformerList.get( key );
        }
        
        final Transformer newTransformer = new DuplicatesTransformer(asynchronous);
        
        duplicatesTransformerList.put(key, newTransformer);
        return newTransformer;
    }
        
}
