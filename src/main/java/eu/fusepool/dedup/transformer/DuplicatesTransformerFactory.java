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
        final String silkConfigUri = getRequestParamValue(request.getQueryString(), SILK_CONFIG_FILE_URI_PARAM);
        final String asyncValue = getRequestParamValue(request.getQueryString(), TRANSFORMER_ASYNC_PARAM);
        if ( ! "".equals(asyncValue) ) {
        	asynchronous = Boolean.getBoolean(asyncValue);
        }
        return getTransfomerFor(request.getQueryString());
    }

    private synchronized Transformer getTransfomerFor(String queryString) {
        if (duplicatesTransformerList.containsKey( queryString )) {
            return duplicatesTransformerList.get( queryString );
        }
        
        final Transformer newTransformer = new DuplicatesTransformer(asynchronous);
        
        duplicatesTransformerList.put(queryString, newTransformer);
        return newTransformer;
    }
    /**
     * Extracts the Silk config file URI from the querystring. When more than one parameter is sent
     * in a POST message using the standard format ?param1=value1&param2=value2" with a content-type 
     * that is not application/x-www-form-urlencoded only the first parameter is returned using an 
     * HttpServletRequest object so it is necessary to parse the full query string to get all the 
     * query parameters.
     * @param queryString
     * @return
     */
    protected String getRequestParamValue(String queryString, String paramName) {
    	String paramValue = "";
    	log.info("Query string: " + queryString);
    	String [] params = queryString.split("&");
    	for(String param: params){
    		if ( paramName.equals( param.split("=")[0]) ) {
    			paramValue = param.split("=")[1];
    		}
    	}
    	log.info(paramName + ": " + paramValue);;
    	return paramValue;
    }
    
}
