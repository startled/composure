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

Similarly, you'll also need the Closure compiler, which is not available on
any well-known Maven repositories at this time. You can download the compiler.jar
from the Closure Compiler site, unzip the file to get the jar, and install it
into Maven with this command

mvn install:install-file -Dfile=compiler.jar -DgroupId=composure -DartifactId=closure-compiler -Dversion=20100616 -Dpackaging=jar

The Closure Compiler will now be available from Leiningen.