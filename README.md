Fusepool P3-SilkDedup
============

A deduplication transformer. Implements the requirements in [FP-106](https://fusepool.atlassian.net/browse/FP-106).
The application enables a user to send an http POST request with RDF data and the url with the rules for the entities disambiguation to find duplicates within it.

[![Build Status](https://travis-ci.org/fusepoolP3/p3-silkdedup.svg)](https://travis-ci.org/fusepoolP3/p3-silkdedup)

Compile the application using the command

    mvn install

To start the application move to the p3-silkdedup/ folder and run the command

    mvn exec:java

The deduplication task is based on a set of rules written in the SILK Link Specification Language (see [1]) and stored in a configuration file. The config file url must be passed as a query parameter to the transformer in the http POST request with the RDF data containing the entities that must be disambiguated. A small data set and a SILK config file is provided in src/test/resources/eu/fusepool/dedup/transformer folder as an example. The file testfoaf.ttl shown below contains different representations and URIs of the same entity 'Barack Obama'

    @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
    @prefix foaf: <http://xmlns.com/foaf/0.1/> .
    @prefix dbpedia-owl: <http://dbpedia.org/ontology/> .
    @prefix dbpedia: <http://dbpedia.org/resource/> .

    <http://example.org/Obama> rdf:type foaf:Person ;
                     foaf:givenName "Barack" ;
                     foaf:familyName "Obama" .

    <http://dbpedia.org/Barack_Obama> rdf:type foaf:Person ;
                     foaf:givenName "Barack" ;
                     foaf:familyname "Obama" ;
                     dbpedia-owl:residence dbpedia:White_House .

    <http://www.whitehouse.gov/Barack_Obama> rdf:type foaf:Person ;
                     foaf:givenName "Barack" ;
                     foaf:familyname "Obama" ;
                     dbpedia-owl:spouse dbpedia:Michelle_Obama .

The properties foaf:givenName and foaf:familyname can be used to compare and disambiguate the different representations of the
entity of rdf:type foaf:Person 'Barack Obama', where foaf is a prefix for the namespace http://xmlns.com/foaf/0.1/. The main part of the rules in silk-config-file.xml is shown below

    <LinkageRule>
         <Aggregate type="average">
            <Compare metric="jaroWinkler">
               <Input path="?person_s/foaf:givenName"/>
               <Input path="?person_t/foaf:givenName"/>
            </Compare>
            <Compare metric="jaroWinkler">
               <Input path="?person_s/foaf:familyName"/>
               <Input path="?person_t/foaf:familyName"/>
            </Compare>
         </Aggregate>
    </LinkageRule>

Jaro-Winkler is the similarity measure used in the example to compare the property values.

To test the application open a new shell, copy the files testfoaf.ttl and silk-config-file in src/test/resources/eu/fusepool/dedup/transformer to a local folder (e.g. /home/user/ ) and run a command like the following

    curl -X POST -H "Content-Type: text/turtle" -T testfoaf.ttl http://localhost:7100/?config=file:///home/user/silk-config-file.xml

If the SILK configuration file can be put in a web server just use its http url in place of the file url. The same example SILK configuration file is available on the Github repository at the URL

https://raw.githubusercontent.com/fusepoolP3/p3-silkdedup/master/src/main/resources/eu/fusepool/dedup/transformer/silk-config-file.xml

The result of the interlinking process, a set of owl:sameAs statements, will be sent back to the client.

[1] https://www.assembla.com/wiki/show/silk/Link_Specification_Language
