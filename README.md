Let's make it easy to work with Closure and Clojure.

Requirements
------------
When/if JDK7 is released, this will be modified to use the file watch
API, and thus become free of dependencies. Until then, this library requires
JPathWatch, which implements the same API. Download JPathWatch from 
http://jpathwatch.wordpress.com, and then install the JAR into Maven with the
command

		mvn install:install-file -Dfile=jpathwatch-0-92a.jar -DgroupId=jpathwatch -DartifactId=jpathwatch -Dversion=0.92a -Dpackaging=jar

JPathWatch will now be available from Leiningen. 