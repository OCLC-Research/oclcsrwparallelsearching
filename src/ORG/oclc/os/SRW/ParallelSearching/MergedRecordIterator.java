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
 * RemoteRecordIterator.java
 *
 * Created on November 10, 2005, 3:17 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

//import ORG.oclc.ber.DataDir;
import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.Record;
import ORG.oclc.os.SRW.RecordIterator;
import ORG.oclc.os.SRW.SRWDiagnostic;
import gov.loc.www.zing.srw.ExtraDataType;
import java.util.NoSuchElementException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author levan
 */
public class MergedRecordIterator implements RecordIterator {
    static Log log=LogFactory.getLog(MergedRecordIterator.class);

//    boolean perhapsCachedResults;
    ExtraDataType edt;
    int numRecs;
    long postings, startPoint;
    MergedQueryResult result;
    RecordIterator ri=null;
    String schemaID;
    
    /** Creates a new instance of RemoteRecordIterator */
    public MergedRecordIterator(MergedQueryResult result, long startPoint,
      int numRecs, String schemaID, ExtraDataType edt) throws InstantiationException {
        if(log.isDebugEnabled())
            log.debug("startPoint="+startPoint+", numRecs="+numRecs+", schemaID="+schemaID);
        this.result=result;
        this.startPoint=startPoint;
        this.numRecs=numRecs;
        this.schemaID=schemaID;
        this.edt=edt;
//        if(startPoint==1)
//            perhapsCachedResults=true;
//        else
//            perhapsCachedResults=false;
        postings=result.getNumberOfRecords();
    }

    public void close() {
    }
    
    public boolean hasNext() {
        if(startPoint<=postings)
            return true;
        return false;
    }

    public Object next() throws NoSuchElementException {
        return nextRecord();
    }

    public Record nextRecord() throws NoSuchElementException {
        try {
            if(ri!=null && ri.hasNext()) {
                startPoint++;
                return ri.nextRecord();
            }
        }
        catch(SRWDiagnostic e) {
            log.error(e, e);
            throw new NoSuchElementException(e.getMessage());
        }

        int partitionNum;
        long actualStartPoint=startPoint;
        QueryResult qr=null;
        for(partitionNum=0; partitionNum<result.db.length; partitionNum++) {
            if(result.db[partitionNum].result.getNumberOfRecords()>=actualStartPoint) {
                qr=result.db[partitionNum].result;
                log.debug("getting QueryResult from partition "+partitionNum);
                break;
            }
            actualStartPoint-=result.db[partitionNum].result.getNumberOfRecords();
        }
        if(qr!=null) {
            try {
                if(log.isDebugEnabled()) {
                    log.debug("newRecordIterator for "+qr);
                    log.debug("actualStartPoint="+actualStartPoint+", numRecs="+numRecs+", schemaID="+schemaID);
                }
                ri=qr.newRecordIterator(actualStartPoint, numRecs, schemaID, edt);
            }
            catch(InstantiationException e) {
                log.error(e, e);
                throw new NoSuchElementException(e.getMessage());
            }
            return nextRecord();
        }
        throw new NoSuchElementException("startPoint="+startPoint+", postings="+postings+", actualStartPoint in partition "+partitionNum+" = "+actualStartPoint);
    }
    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
