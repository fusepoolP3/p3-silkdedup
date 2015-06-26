SilkDedup Transformer [![Build Status](https://travis-ci.org/fusepoolP3/p3-silkdedup.svg)](https://travis-ci.org/fusepoolP3/p3-silkdedup)
=====================

A deduplication and interlinking transformer. Implements the requirements specified in [FP-106](https://fusepool.atlassian.net/browse/FP-106). 

The difference between a deduplication task and an interlinking task lies in the purpose and is related to the source and target data source. If they are the same the task is said deduplication as the purpose is to find duplicate entities within the same data set for which no differences in the descriptions are expected. The purpose is not to merge information but to remove redundancy. In an interlinking task the source and target data source are different and different information about an entity are expected in the two data sets so that the purpose is to merge them. The result in both case is the same, a set of 0 or more pairs of equivalent entities. 

The interlinking and deduplication tasks are based on a set of rules written in the SILK Link Specification Language (see [SILK specification](https://www.assembla.com/wiki/show/silk/Link_Specification_Language)) and stored in a configuration file. The config file URL must be passed as a query parameter to the transformer in the http POST request with the RDF data containing the entities that must be deduplicated/interlinked. The client RDF data is always used as the source data source, of type "file", for the comparisons with a target data source. The target data source can be of type "file" or "sparqlEndpoint". If the target data source is of type "file" then the same client data will be used as target data source (deduplication).

## Try it out
The transformer can be started using the latest release that can be downloaded from the releases section. The executable jar file contains all the necessary dependencies. To start an instance of the transformer factory run the command

      java -jar p3-silkdedup-v1.0.0-jar-with-dependencies.jar
    
An instance of the SilkDedup transformer factory will be listening at the default port 8306. The port number can be changed, for example to use port number 7100, as follows

     java -jar p3-silkdedup-v1.0.0-jar-with-dependencies.jar -P 7100
     
## Compiling and Running 
Compile the Maven project using the command

    mvn install

To start the application move to the p3-silkdedup/ folder and run the command

    mvn exec:java

The default port number used by the transformer when started with Maven is set in the pom.xml file.

## Usage

A small data set and a SILK config file is provided in src/test/resources/eu/fusepool/dedup/transformer folder as an example. The file testfoaf.ttl shown below contains different representations and URIs of the same entity 'Barack Obama'

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

    curl -X POST -H "Content-Type: text/turtle" -d @testfoaf.ttl "http://localhost:7100/?config=file:///home/user/silk-config-file.xml"

The factory will look whether a request with the same URL has been already submitted and a transformer is already available to handle it. In case it is a new request the factory will create a new instance of the transformer. If the SILK configuration file can be put in a web server just use its http url in place of the file url. The same example SILK configuration file is available on the Github repository at the URL

https://raw.githubusercontent.com/fusepoolP3/p3-silkdedup/master/src/main/resources/eu/fusepool/dedup/transformer/silk-config-file.xml

The result of the interlinking process, a set of owl:sameAs statements is added to each entity representation and sent back to the client.

    <http://example.org/Obama>
              a       <http://xmlns.com/foaf/0.1/Person> ;
              <http://www.w3.org/2002/07/owl#sameAs>
                      <http://www.whitehouse.gov/Barack_Obama> , <http://dbpedia.org/Barack_Obama> , <http://example.org/Obama> ;
              <http://xmlns.com/foaf/0.1/familyName> "Obama" ;
              <http://xmlns.com/foaf/0.1/givenName> "Barack" .

              
A transformer responds synchronously by default to a request. The client can change the execution mode of a new transformer instance sending an additional parameter "async=true" to the post request. The previous command would be

    curl -i -X POST -H "Content-Type: text/turtle" -d @testfoaf.ttl "http://localhost:7100/?config=file:///home/user/silk-config-file.xml&async=true"
    
The server will send a message to the client with a "Location" header with the relative path of the resource that will be created as a result 

    HTTP/1.1 202 Accepted
    Date: Fri, 26 Jun 2015 14:59:09 GMT
    Location: /job/fbf1082c-1b2d-4df5-96e6-332dce8b4b6c
    Transfer-Encoding: chunked
    Server: Jetty(9.2.z-SNAPSHOT)

The resource created by the transformer will be available at the URL http://localhost:7100/job/fbf1082c-1b2d-4df5-96e6-332dce8b4b6c

    curl http://localhost:7100/job/fbf1082c-1b2d-4df5-96e6-332dce8b4b6c
              