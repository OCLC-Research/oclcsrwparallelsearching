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
 * QueryBucket.java
 *
 * Created on August 10, 2004, 2:11 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

/**
 *
 * @author  levan
 */
public class QueryBucket {
    private int numDone, numThreads;
    
    boolean quit;
    int    queryID=-1;
    long   expectedValue;
    String query;

    QueryBucket() {
    }

    QueryBucket(int numThreads) {
        this.numThreads=numDone=numThreads;
    }
    
    synchronized void done() {
        numDone++;
        if(numDone==numThreads)
            notifyAll();
    }

    public boolean getQuit() {
        return quit;
    }

    public synchronized void quit() {
        queryID++;
        numDone=0;
        quit=true;
        notifyAll();
    }

    void setNumThreads(int numThreads) {
        this.numThreads=numThreads;
    }

    synchronized void setQuery(String s, int queryID) {
//        System.out.println("QueryBucket.setQuery: queryID="+queryID+", s="+s);
        query=s;
        expectedValue=-1;
        this.queryID=queryID;
        numDone=0;
        notifyAll();
    }

    synchronized void setQuery(String s, long expectedValue, int queryID) {
//        System.out.println("QueryBucket.setQuery: queryID="+queryID+", s="+s);
        query=s;
        this.expectedValue=expectedValue;
        this.queryID=queryID;
        numDone=0;
        notifyAll();
    }

    synchronized void waitForNewQuery(int lastQueryID) {
        while(numDone==numThreads || queryID==lastQueryID)
            try {
                wait();
            }
            catch(InterruptedException e) {}
    }

    synchronized void waitUntilDone() {
        while(numDone<numThreads)
            try {
                wait();
            }
            catch(InterruptedException e) {}
    }
}
