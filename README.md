Fusepool P3-SilkDedup
============

A deduplication transformer. Implements the requirements in FP-106.

The apllication enables a user to send an http POST request with RDF data to find duplicates within it. 

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