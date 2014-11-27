/*
 * Copyright 2014 Bern University of Applied Sciences.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.dedup.transformer;


import eu.fusepool.p3.transformer.sample.Arguments;
import eu.fusepool.p3.transformer.server.TransformerServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

import org.wymiwyg.commons.util.arguments.ArgumentHandler;

/**
 *
 * @author reto
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Arguments arguments = ArgumentHandler.readArguments(Arguments.class, args);
        if (arguments != null) {
            start(arguments);
        }
    }

    private static void start(Arguments arguments) throws Exception {
    	
        TransformerServer server = new TransformerServer(arguments.getPort());
        InputStream in = Main.class.getResourceAsStream("bla.txt");
        
        server.start(new DuplicatesTransformer());
       
        server.join();
    }
    
}
