Fusepool P3-SilkDedup
============

A deduplication transformer. Implements the requirements in [FP-106](https://fusepool.atlassian.net/browse/FP-106).

The apllication enables a user to send an http POST request with RDF data to find duplicates within it. Compile the application using the command

    mvn install

To start the application move to the p3-silkdedup/ folder and run the command

    mvn exec:java

The deduplication task is based on a set of rules written in the SILK Link Specification Language. The rules currently implemented can be used to disambiguate descriptions of entities of type foaf:Person using the properties 
foaf:givenName and foaf:familyName where foaf is a prefix for the namespace http://xmlns.com/foaf/0.1/

To test the application open a new shell, go to src/test/resources/eu/fusepool/dedup/transformer folder and run the following command 

    curl -X POST -T testfoaf.ttl http://localhost:7100

The file testfoaf.ttl contains different representations of the same entity Barack Obama. The result of the interlinking process, a set of owl:sameAs statements, will be sent back to the client.
The SILK configuration file can be fetched by its URL like in the following example

    curl -X POST -T testfoaf.ttl http://localhost:7100/?config=<SILK config file URL>
