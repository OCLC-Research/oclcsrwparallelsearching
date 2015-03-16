# Build Instructions #

Instructions for how to build the Federated searching interface for the SRW/SRU server.

## Prerequisites ##
  * Java 1.5 or higher
  * Tomcat or some other servlet engine
  * Ant

## Step 1 ##
Download the latest Federated searching interface from the SVN repository.  (Instructions for that can be found [here](http://code.google.com/p/oclcsrwparallelsearching/source/checkout).)

## Step 2 ##
Run "ant" to compile the code and build SRWParallelSearching.jar.

## Step 3 ##
After you’ve run “ant”, you’ll find a directory named “dist” and in it you will find the SRWParallelSearching.jar file. If you are building a new complete SRW server, then copy the jar file from the "dist" directory to the web/WEB-INF/lib directory of the SRW distribution.  If you are just updating an already deployed SRW server, then copy those same files to your `<tomcat>/webapps/<SRW>/WEB-INF/lib` directory.

# Testing #
# Ignore the section below #
It is obviously copied from the SRWLucene documentation and is a placeholder to help me remember to put together a testing framework.  The likely test will be to set up a search across multiple copies of the builtin test database.

The SRWLucene package included the Lucene demo database build from their documentation.  If you'd like to test with it, edit the SRWServer.props file in the `<tomcat>/webapps/SRW/WEB-INF/classes` directory and change the path in the db.testlucene.home property.  After making that change, start (or restart) your tomcat server and the following links (changed as necessary for your installation) should work.

http://localhost:8080/SRW/search/lucenetest should get you something like this:

![http://oclcsrw.googlecode.com/svn/wiki/images/LuceneTestExplainResponse.jpg](http://oclcsrw.googlecode.com/svn/wiki/images/LuceneTestExplainResponse.jpg)

http://localhost:8080/SRW/search/lucenetest?query=local.contents=dog should get you something like this:

![http://oclcsrw.googlecode.com/svn/wiki/images/LuceneTestSearchResponse.jpg](http://oclcsrw.googlecode.com/svn/wiki/images/LuceneTestSearchResponse.jpg)

http://localhost:8080/SRW/search/lucenetest?scanClause=local.contents=dog should get you something like this:

![http://oclcsrw.googlecode.com/svn/wiki/images/LuceneTestScanResponse.jpg](http://oclcsrw.googlecode.com/svn/wiki/images/LuceneTestScanResponse.jpg)