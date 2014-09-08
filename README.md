Fusepool P3-SilkDedup
============

A deduplication transformer. Implements the requirements in [FP-106](https://fusepool.atlassian.net/browse/FP-106).

The apllication enables a user to send an http POST request with RDF data to find duplicates within it. Compile the application using the command

    mvn install

To start the application move to the p3-silkdedup/deduptransformer/ folder and run the command

    mvn exec:java

The deduplication task is based on a set of rules written in the SILK Link Specification Language. The rules currently implemented can be used to disambiguate descriptions of entities of type foaf:Person using the properties 
foaf:givenName and foaf:familyName where foaf is a prefix for the namespace http://xmlns.com/foaf/0.1/

To test the application open a new shell, go to the deduptransformer/src/test/resources/ folder and run the following command 

    curl -X POST -T testfoaf.ttl http://localhost:7100

The file testfoaf.ttl contains different representations of the same entity Barack Obama. The result of the interlinking process, a set of owl:sameAs statements, will be sent back to the client.

The SILK version used is 2.6.0 that is not available from the Maven repository. In order to use it in Maven it must be dowloaded from the project webste

http://wifo5-03.informatik.uni-mannheim.de/bizer/silk/

then unzip the file and import the file silk.jar into your local Maven repository using the command

mvn install:install-file -Dfile=&lt; path to silk.jar &gt; -DgroupId=de.fuberlin.wiwiss.silk -DartifactId=silk -Dversion=2.6.0 -Dpackaging=jar

The silk.jar file can be used as a dependency in the project using the coordinates

    &lt;dependency&gt;  
      &lt;groupId&gt;de.fuberlin.wiwiss.silk&lt;/groupId&gt;  
      &lt;artifactId&gt;silk&lt;/artifactId&gt;  
      &lt;version&gt;2.6.0&lt;/version&gt;  
    &lt;/dependency&gt;  
