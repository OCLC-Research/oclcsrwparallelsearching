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
 * SRWDatabaseThread.java
 *
 * Created on November 19, 2002, 1:53 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.SRWDatabase;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;

import org.apache.axis.types.NonNegativeInteger;
import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author  levan
 */
public class SRWDatabaseThread extends Thread {
    static Log log=LogFactory.getLog(SRWDatabaseThread.class);
    static int numInstances=0;

//    public Object response;

    int           instanceNum, lastRequestID, whoAmI;
    QueryResult result;
    RequestBucket rb;
    ScanResponseType scanResponse;
    SRWDatabase db;
    boolean didIt;

    public SRWDatabase getDB() {
        return db;
    }

    private void handleSearch(SearchRetrieveRequestType request) throws Exception {
            result=db.getQueryResult(request.getQuery(), request);
            log.info(getName()+": postings="+result.getNumberOfRecords());
    }

    public void init(RequestBucket rb, int whoAmI, SRWDatabase db) throws Exception {
        log.info("init for db: "+db);
        this.rb=rb;
        rb.register(this);
        this.whoAmI=whoAmI;
        lastRequestID=rb.requestID;
        instanceNum=numInstances++;
        setName(db.dbname+"("+instanceNum+")");
        this.db=db;
    }

    public void run() {
        log.info("enter run");
        while(true) {
            rb.waitForNewRequest(lastRequestID);
            if(rb.quit) {
                log.info("exit run");
                return;
            }
            lastRequestID=rb.requestID;
            try {
                if(rb.request instanceof SearchRetrieveRequestType) {
                    SearchRetrieveRequestType request=(SearchRetrieveRequestType)rb.request;
                    result=db.getQueryResult(request.getQuery(), request);
                    log.info(getName()+": postings="+result.getNumberOfRecords());
                }
                else if(rb.request instanceof ScanRequestType)
                    scanResponse=db.doRequest((ScanRequestType)rb.request);
                else if(rb.request instanceof DeleteRequest) {
                    String key=((DeleteRequest)rb.request).getKey();
                    System.out.println("in SRWDatabaseThread.run(): deleting key "+key+" from database "+db.dbname);
                    didIt=db.delete(key);
                    System.out.println("in SRWDatabaseThread.run(): "+(didIt?"success!":"failure"));
                }
            }
            catch(Exception e) {
                log.error(e, e);
            }
            finally {
                rb.done(this);
            }
        }
    }

    public void test(String query) {
        try {
            SearchRetrieveRequestType request=new SearchRetrieveRequestType();
            request.setQuery(query);
            request.setResultSetTTL(new NonNegativeInteger("0"));
            request.setMaximumRecords(new NonNegativeInteger("0"));
            result=db.getQueryResult(request.getQuery(), request);
//            response=db.doRequest(request);
            log.info(getName()+": postings="+result.getNumberOfRecords());
        }
        catch(Exception e) {
            log.error(e, e);
        }
    }
}
