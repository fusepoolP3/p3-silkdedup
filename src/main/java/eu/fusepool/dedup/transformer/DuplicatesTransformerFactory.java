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

/**
 *
 * @author reto
 */
public class DuplicatesTransformerFactory implements TransformerFactory {
	private boolean asynchronous = false; //set the transformer execution mode to asynchronous when set to true
    private final Map<String, DuplicatesTransformer> duplicatesTransformerList = 
            new HashMap<>();
    
    @Override
    public DuplicatesTransformer getTransformer(HttpServletRequest request) {
        final String silkConfigUri = request.getParameter("config");
        String async = request.getParameter("asynchronous");
        System.out.println("Factory transformer async: " + async);
        if ( "true".equals( async ) ) {
        	asynchronous = true;
        }
        
        return getTransfomerFor(silkConfigUri);
    }

    private synchronized DuplicatesTransformer getTransfomerFor(String silkConfigUri) {
        if (duplicatesTransformerList.containsKey( silkConfigUri )) {
            return duplicatesTransformerList.get( silkConfigUri );
        }
        final DuplicatesTransformer newTransformer = new DuplicatesTransformer(asynchronous);
        
        duplicatesTransformerList.put(silkConfigUri, newTransformer);
        return newTransformer;
    }
    
}
