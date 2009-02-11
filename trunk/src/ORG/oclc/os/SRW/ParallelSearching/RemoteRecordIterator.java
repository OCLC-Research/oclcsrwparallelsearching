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

import ORG.oclc.os.SRW.Record;
import ORG.oclc.os.SRW.RecordIterator;
import gov.loc.www.zing.srw.ExtraDataType;
import gov.loc.www.zing.srw.RecordType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import java.util.NoSuchElementException;
import org.apache.axis.message.MessageElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author levan
 */
public class RemoteRecordIterator implements RecordIterator {
    static Log log=LogFactory.getLog(RemoteRecordIterator.class);
    ExtraDataType edt;
    int     offset, recordCount=0, resultSetIdleTime=0;
    long    postings, startPoint;
    RecordType records[];
    RemoteQueryResult result;
    SearchRetrieveResponseType response;
    String resultSetID=null, schemaID;
    
    /** Creates a new instance of RemoteRecordIterator */
    public RemoteRecordIterator(RemoteQueryResult result, long startPoint,
      int numRecs, String schemaID, ExtraDataType edt) throws InstantiationException {
        this.result=result;
        this.startPoint=startPoint;
        this.schemaID=schemaID;
        this.edt=edt;
        response=result.response;
        offset=0;
        postings=response.getNumberOfRecords().longValue();
        records=response.getRecords().getRecord();
        resultSetID=response.getResultSetId();
        if(response.getResultSetIdleTime()!=null)
            resultSetIdleTime=response.getResultSetIdleTime().intValue();
    }

    public void close() {
    }

    public boolean hasNext() {
        if(records==null) {
            log.info("records==null");
            return false;
        }
        if(offset+1>records.length) {
            log.info("offset+1="+(offset+1)+", records.length="+records.length);
            return false;
        }
        if(startPoint+offset<=postings)
            return true;

        log.info("startPoint="+startPoint+", offset="+offset+", postings="+postings);
        return false;
    }

    public Object next() throws NoSuchElementException {
        return nextRecord();
    }

    public Record nextRecord() throws NoSuchElementException {
        if(!hasNext())
            throw new NoSuchElementException("offset="+offset+", records.length="+records.length);
        RecordType rec=records[offset++];
        if(log.isDebugEnabled())
            log.debug("rec="+rec);
        String schema=rec.getRecordSchema();

        MessageElement elems[]=rec.getRecordData().get_any();
        String stringRecord=elems[0].toString();
        if(log.isDebugEnabled())
            log.debug("record after get_any: "+stringRecord);

        Record record=new Record(stringRecord, schema);
        return record;
    }
    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
