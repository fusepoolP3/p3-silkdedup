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

    private final Map<String, Transformer> duplicatesTransformerList = 
            new HashMap<>();
    
    @Override
    public Transformer getTransformer(HttpServletRequest request) {
        final String silkConfigUri = request.getParameter("config");
        return getTransfomerFor(silkConfigUri);
    }

    private synchronized Transformer getTransfomerFor(String silkConfigUri) {
        if (duplicatesTransformerList.containsKey( silkConfigUri )) {
            return duplicatesTransformerList.get( silkConfigUri );
        }
        final Transformer newTransformer = new DuplicatesTransformer();
        duplicatesTransformerList.put(silkConfigUri, newTransformer);
        return newTransformer;
    }
    
}
