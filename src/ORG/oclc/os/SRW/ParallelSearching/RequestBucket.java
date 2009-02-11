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
 * RequestBucket.java
 *
 * Created on July 30, 2004, 2:28 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ORG.oclc.os.SRW.QueryResult;

/**
 *
 * @author  levan
 */
public class RequestBucket {
    static Log log=LogFactory.getLog(RequestBucket.class);

    private int numDone, numThreads;

    public boolean      quit=false;
    public int          requestID=-1;
//    public QueryResult result[];
    public QueryResult results[];
    public Object request;


    Hashtable registry=new Hashtable(), waitList;

    RequestBucket(int numThreads) {
        this.numThreads=numDone=numThreads;
        log.info("numThreads="+numThreads);
        results=new QueryResult[numThreads];
//        dbs=new SRWDatabaseImpl[numThreads];
    }
    
    synchronized void waitUntilDone() {
        log.info("entering waitUntilDone: numDone="+numDone+", numThreads="+numThreads);
        while(numDone<numThreads) {
            try {
                wait();
            }
            catch(InterruptedException e) {}
            log.info("waiting in waitUntilDone: numDone="+numDone+", numThreads="+numThreads);
        }
    }
    
    synchronized void done(SRWDatabaseThread t) {
        numDone++;
        log.info(t+" done "+"("+numDone+" of "+numThreads+")");
        if(log.isDebugEnabled()) {
            waitList.remove(t);
            log.info("waiting for: "+waitList);
        }
        if(numDone==numThreads)
            notifyAll();
    }

    synchronized void register(SRWDatabaseThread t) {
        registry.put(t, t);
    }

    synchronized void setRequest(Object req, int requestID) {
        if(log.isDebugEnabled())
            waitList=(Hashtable)registry.clone();
        request=req;
        this.requestID=requestID;
        numDone=0;
        notifyAll();
    }

    synchronized void waitForNewRequest(int lastRequestID) {
        log.debug("enter waitForNewRequest: lastRequestID="+lastRequestID+", requestID="+requestID);
        while(numDone==numThreads || requestID==lastRequestID)
            try {
                wait();
            }
            catch(InterruptedException e) {}
    }
}
