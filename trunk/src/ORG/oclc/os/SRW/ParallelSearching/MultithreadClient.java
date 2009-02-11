/*
   Copyright 2006 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/*
 * MultithreadClient.java
 *
 * Created on November 19, 2002, 1:53 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;


import ORG.oclc.os.SRW.Utilities;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

/**
 *
 * @author  levan
 */
public class MultithreadClient extends Thread {
    static boolean quiet=false;
    static int     maxRecords=0, totalNumSearches=0;
    static LinkedList pool=new LinkedList();
    static FileWriter out=null;
    
    boolean     available=true;
    int         counter=0, lastQueryID=-1, numExpectationErrors=0, numScans=0, numSearches=0;
    long        postings, totalElapsed=0, totalElapsedScanning=0,
                totalElapsedSearching=0, totalPostings=0;
    QueryBucket qb=new QueryBucket(1);
    String      scanUrl, searchUrl;
    

    public MultithreadClient(String urlString) throws Exception {
        scanUrl  =urlString+"?version=1.1&maximumTerms=20&responsePosition=10&scanClause=";
        searchUrl=urlString+"?version=1.1&startRecord=1&resultSetTTL=0&query=";
    }

    public void reset() {
        numSearches=numScans=0;
        totalElapsed=totalElapsedScanning=totalElapsedSearching=totalPostings=0;
    }

    public void run() {
        boolean foundCount;
        BufferedReader in=null;
        int start, stop;
        long begin, elapsed;
        String encodedQuery, firstTerm, inputLine, urlStr;
        URL u=null;
        while(true) {
            qb.waitForNewQuery(lastQueryID);
            if(qb.quit) {
//                System.out.println(getName()+": done");
                return;
            }
            lastQueryID=qb.queryID;
            encodedQuery=Utilities.urlEncode(qb.query);
            try {
                begin=System.currentTimeMillis();
                if(encodedQuery.startsWith("b+")) { // browse
                    urlStr=scanUrl+encodedQuery.substring(2);
                    u=new URL(urlStr);
                    in=new BufferedReader(new InputStreamReader(u.openStream()));
                    firstTerm="no first term";
                    while ((inputLine = in.readLine()) != null) {
                        start=inputLine.indexOf("<term>");
                        if(start>=0) {
                            stop=inputLine.indexOf("</term>", start);
                            firstTerm=inputLine.substring(start, stop);
                        }
                    }
                    numScans++;
                    elapsed=System.currentTimeMillis()-begin;
                    totalElapsedScanning+=elapsed;
                    if(!quiet)
                        System.out.println("firstTerm="+firstTerm+" ("+elapsed+"ms)");
                    if(out!=null) {
                        out.write(qb.query+"\n");
                        out.write("-1\n");
                    }
                }
                else { // search
                    urlStr=searchUrl+encodedQuery+"&maximumRecords="+maxRecords;
                    if(urlStr.length()>255) {
                        System.out.println("*** skipping due to length: "+urlStr);
                    }
                    else {
                        u=new URL(urlStr);
                        in=new BufferedReader(new InputStreamReader(u.openStream()));
                        foundCount=false;
                        while ((inputLine = in.readLine()) != null) {
                            start=inputLine.indexOf("<numberOfRecords");
        //                    System.out.println(inputLine);
                            if(start>=0) {
                                start+=16;
                                while(inputLine.charAt(start)!='>')
                                    start++;
                                start++;
                                stop=inputLine.indexOf('<', start);
                                postings=Integer.parseInt(inputLine.substring(start, stop));
                                if(qb.expectedValue>=0 && postings!=qb.expectedValue) {
                                    numExpectationErrors++;
                                    System.out.println("*** expected "+qb.expectedValue+" for \""+qb.query+"\"");
                                }
                                foundCount=true;
                                break;
                            }
                        }
                        if(!foundCount)
                            System.out.println("no count for \""+qb.query+"\"?");
                        in.close();
                        totalPostings+=postings;
                        numSearches++;
                    }
                    elapsed=System.currentTimeMillis()-begin;
                    if(!quiet)
                        System.out.println((++totalNumSearches)+": query=\""+qb.query+"\", postings="+postings+" ("+elapsed+"ms)");
                    totalElapsedSearching+=elapsed;
                    if(out!=null) {
                        out.write(qb.query+"\n");
                        out.write(Long.toString(postings)+"\n");
                    }
                }
                totalElapsed+=elapsed;
            }
            catch(Exception e) {
                e.printStackTrace();
                System.out.println("url="+u);
                System.exit(99);
            }
            qb.done();
            pool.addLast(this);
            available=true;
        }
    }

    public void search(String query) {
        available=false;
        qb.setQuery(query, ++counter);
    }

    public void search(String query, long expectedValue) {
        available=false;
        qb.setQuery(query, expectedValue, ++counter);
    }

    public static void main(String[] args) throws Exception {
        boolean        expecting=false;
        BufferedReader in=null, urls=null;
        int i, maxSearches=Integer.MAX_VALUE, numClients=10;
        String inputFileName=null, line, outputFileName=null, urlFileName;

        for(i=0; i<args.length; i++) {
            if(args[i].charAt(0)=='-') {
                switch(args[i].charAt(1)) {
                    case 'c':
                        try {
                            if(args[i].length()==2) // look in next arg
                                numClients=Integer.parseInt(args[++i]);
                            else                        
                                numClients=Integer.parseInt(args[i].substring(2));
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case 'e':
                        expecting=true;
                        break;
                        
                    case 'i':
                        if(args[i].length()==2) // look in next arg
                            inputFileName=args[++i];
                        else                        
                            inputFileName=args[i].substring(2);
                        in=new BufferedReader(new FileReader(inputFileName));
                        break;

                    case 'm':
                        try {
                            if(args[i].length()==2) // look in next arg
                                maxRecords=Integer.parseInt(args[++i]);
                            else                        
                                maxRecords=Integer.parseInt(args[i].substring(2));
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case 'n':
                        try {
                            if(args[i].length()==2) // look in next arg
                                maxSearches=Integer.parseInt(args[++i]);
                            else                        
                                maxSearches=Integer.parseInt(args[i].substring(2));
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case 'o':
                        if(args[i].length()==2) // look in next arg
                            outputFileName=args[++i];
                        else                        
                            outputFileName=args[i].substring(2);
                        break;

                    case 'q':
                        quiet=true;
                        break;
                        
                    case 'u':
                        if(args[i].length()==2) // look in next arg
                            urlFileName=args[++i];
                        else                        
                            urlFileName=args[i].substring(2);
                        urls=new BufferedReader(new FileReader(urlFileName));
                        break;
                }
            }
        }

        if(urls==null) {
            System.out.println("urls file not specified (-u parm)");
            return;
        }
        if(in==null) {
            System.out.println("input file not specified (-i parm)");
            return;
        }
        MultithreadClient client, clients[]=new MultithreadClient[numClients];
        System.out.println("creating "+numClients+" clients");
        String lastUrl=null, url;
        for(i=0; i<numClients; i++) {
            url=urls.readLine();
            if(url==null)
                url=lastUrl;
            lastUrl=url;
            clients[i]=client=new MultithreadClient(url);
            client.start();
//            client.search("bogus");
//            while(!client.available)
//                sleep(100);
            client.reset();
            pool.addLast(client);
        }
        System.out.println("priming clients with bogus search");
        for(i=0; i<numClients; i++) {
            client=(MultithreadClient)pool.removeFirst();
            client.search("bogus");
        }
        System.out.println("waiting for priming to complete");
        while(pool.size()<numClients) {
            if(pool.size()>0)
                System.out.println(pool.size());
            sleep(100);
        }
        System.out.println("resetting clients");
        for(i=0; i<numClients; i++)
            clients[i].reset();
        System.out.println("done resetting clients");

        if(outputFileName!=null)
            out=new FileWriter(outputFileName);

        long startTime=System.currentTimeMillis();
        int clientNum, numExpectationErrors=0, numScans=0, numSearches=0, thisWaitTime;
        if(in!=null)
            while((line=in.readLine())!=null && totalNumSearches<maxSearches) {
                client=null;
                while(client==null) {
//                    System.out.println("trying to pop a client");
//                    try { // get a client from the pool
                        if(pool.size()>0)
                            client=(MultithreadClient)pool.removeFirst();
//                    }
//                    catch(java.util.EmptyStackException e) {
//                    }
                }
                if(line.indexOf("urlStr=")>=0) { // script is from a log
                    int end, start=line.indexOf("&query=");
                    if(start>=0) {
                        end=line.indexOf("&", start+1);
                        line=Utilities.unUrlEncode(line.substring(start+7, end));
                    }
                    else {
                        start=line.indexOf("&scanClause=");
                        end=line.indexOf("&", start+1);
                        line="b "+Utilities.unUrlEncode(line.substring(start+12, end));
                    }
                }
                if(expecting)
                    client.search(line, Long.parseLong(in.readLine()));
                else
                    client.search(line);
            }
        numSearches=0;
        long totalElapsed=0, totalElapsedScanning=0, totalElapsedSearching=0, totalPostings=0;
        for(int numWaits=0; numWaits<100 && (pool.size()!=numClients); numWaits++) {
            System.out.println("pool.size()="+pool.size()+", numClients="+numClients);
            sleep(100);
        }
        long endTime=System.currentTimeMillis();
        while(!pool.isEmpty()) {
            client=(MultithreadClient)pool.removeFirst();
            client.qb.quit();
            if(client.totalElapsed==0)  // did nothing
                continue;
            
            System.out.println("popped client: numSearches="+client.numSearches+
                ", totalElapsed="+client.totalElapsed+", "+(client.numSearches*1000/client.totalElapsed)+" searches/second");
            numScans+=client.numScans;
            numSearches+=client.numSearches;
            numExpectationErrors+=client.numExpectationErrors;
            totalElapsed+=client.totalElapsed;
            totalElapsedScanning+=client.totalElapsedScanning;
            totalElapsedSearching+=client.totalElapsedSearching;
            totalPostings+=client.totalPostings;
        }
        if(out!=null)
            out.close();
        System.out.println("MultithreadClient report:");
        if(numSearches==0)
            System.out.println("avg. Search Time=0, avgPostings=0, numSearches=0");
        else
            System.out.println("avg. Search Time="+(totalElapsedSearching/numSearches)+
                ", avgPostings="+(totalPostings/numSearches)+
                ", numSearches="+numSearches);
        if(numScans==0)
            System.out.println("avg. Scan Time=0, numScans=0");
        else
            System.out.println("avg. Scan Time="+(totalElapsedScanning/numScans)+
                ", numScans="+numScans);
        System.out.println("numClients="+numClients);
        if(expecting)
            System.out.println("number of expectation errors="+numExpectationErrors);
        System.out.println("elapsed time: "+((endTime-startTime)/1000.0));
        System.out.println("transactions/second="+(((numSearches+numScans)*1000)/(endTime-startTime)));
    }
}
