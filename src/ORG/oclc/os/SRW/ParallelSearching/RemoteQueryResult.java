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
 * RemoteQueryResult.java
 *
 * Created on November 10, 2005, 1:58 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.RecordIterator;
import ORG.oclc.os.SRW.Utilities;
import gov.loc.www.zing.srw.ExtraDataType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author levan
 */
public class RemoteQueryResult extends QueryResult {
    static Log log=LogFactory.getLog(SRWRemoteDatabase.class);
    String remoteResponse;
    String  baseURL;
    SearchRetrieveResponseType response;

    /** Creates a new instance of RemoteQueryResult */
    public RemoteQueryResult(String baseURL, String remoteResponse) throws InstantiationException {
        this.baseURL=baseURL;
        this.remoteResponse=remoteResponse;
        try {
            response=(SearchRetrieveResponseType)Utilities.xmlToObj(remoteResponse);
        }
        catch(Exception e) {
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }
    }
    public long getNumberOfRecords() {
        if(remoteResponse!=null)
            return response.getNumberOfRecords().longValue();
        return 0;
    }
    public int getResultSetIdleTime() {
        if(remoteResponse!=null) {
            NonNegativeInteger rsit=response.getResultSetIdleTime();
            if(rsit!=null)
                return rsit.intValue();
        }
        return 0;
    }

    public RecordIterator newRecordIterator(long startPoint, int numRecs, String schemaID, ExtraDataType edt) throws InstantiationException {
        return new RemoteRecordIterator(this, startPoint, numRecs, schemaID, edt);
    }
}
