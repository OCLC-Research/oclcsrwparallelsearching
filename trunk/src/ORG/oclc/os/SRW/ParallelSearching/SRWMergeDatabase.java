/*
   Copyright 2006 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the Licenfse is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/*
 * SRWMergeDatabase.java
 *
 * Created on July 30, 2004, 4:17 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.os.SRW.*;
import ORG.oclc.os.SRW.QueryResult;
import gov.loc.www.zing.srw.DiagnosticsType;
import gov.loc.www.zing.srw.RecordType;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.StringOrXmlFragment;
import gov.loc.www.zing.srw.TermType;
import gov.loc.www.zing.srw.TermsType;

import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import org.apache.axis.message.MessageElement;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.axis.utils.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLTermNode;


/**
 *
 * @author  levan
 */
public class SRWMergeDatabase extends SRWDatabase {
    static Log log=LogFactory.getLog(SRWMergeDatabase.class);

    Hashtable info=new Hashtable();
    int                 counter=0;
    RequestBucket       rb;
    SRWDatabaseThread[] mergeDB;

    public void addRenderer(String schemaName, String schemaID, Properties props) throws InstantiationException {
    }
    
    public static void addRecordToVector(RecordType originalRecord, long position,
      String recordPacking, Vector recordV)
      throws IOException, ParserConfigurationException, SAXException, SOAPException {
        RecordType record=new RecordType();
        record.setRecordPacking(recordPacking);
        record.setRecordPosition(new PositiveInteger(Long.toString(position)));
        record.setRecordSchema(originalRecord.getRecordSchema());
        MessageElement elems[]=originalRecord.getRecordData().get_any();
        String stringRecord=elems[0].toString();
        if(log.isDebugEnabled())
            log.debug("stringRecord="+stringRecord);
        log.debug("recordPacking"+recordPacking);
        StringOrXmlFragment frag=new StringOrXmlFragment();
        if(recordPacking==null || recordPacking.equals("xml")) {
            Document domDoc=XMLUtils.newDocument(
                new InputSource(
                new StringReader(Utilities.unXmlEncode(stringRecord))));
            elems=new MessageElement[1];
            elems[0]=new MessageElement(
                domDoc.getDocumentElement());
            frag.set_any(elems);
        }
        else { // srw
//            elems=new MessageElement[1];
//            elems[0]=new MessageElement();
//            elems[0].addTextNode(stringRecord);
            frag.set_any(elems);
        }
        record.setRecordData(frag);
        recordV.add(record);
    }

    private int compare(Object a, TermType b) {
        if(a instanceof String)
            return ((String)a).compareTo(b.getValue());
        return ((TermType)a).getValue().compareTo(b.getValue());
    }

    public boolean delete(String key) {
        if(log.isDebugEnabled()) log.debug("in SRWMergeDatabase.delete(): deleting key "+key);
        boolean didIt=false;
        rb.setRequest(new DeleteRequest(key), ++counter);
        rb.waitUntilDone();
        for(int i=0; i<mergeDB.length; i++) {
            if(mergeDB[i]==null)
                log.error("mergeDB["+i+"] is null!");
            else {
                if(log.isDebugEnabled()) log.debug("deleted from "+mergeDB[i].db.dbname+": "+mergeDB[i].didIt);
                if(mergeDB[i].didIt)
                    didIt=true;
            }
        }
        return didIt;
    }

    public TermList getTermList(CQLTermNode seedTerm, int position,
      int maxTerms, ScanRequestType request) {
        DiagnosticsType diagnostics, newDiagnostics=null;
        long startTime=System.currentTimeMillis();
//        position-=1;  // make it zero ordinal
        rb.setRequest(request, ++counter);
        rb.waitUntilDone();
        int         bestFit, comp=1,
                    vectorIndex;
        long        count;
        String scanTerm="";
        try {
            CQLNode node=parser.parse(request.getScanClause());
            if(node instanceof CQLTermNode) {
                scanTerm=((CQLTermNode)node).getTerm();
                if(log.isDebugEnabled())
                    log.debug("scanTerm="+scanTerm);
            }
        }
        catch(org.z3950.zing.cql.CQLParseException e) {
            log.error(e, e);
            TermList tl=new TermList();
            tl.addDiagnostic(SRWDiagnostic.newDiagnosticType(SRWDiagnostic.QuerySyntaxError, e.getMessage()));
            return tl;
        }
        catch(java.io.IOException e) {
            log.error(e, e);
            TermList tl=new TermList();
            tl.addDiagnostic(SRWDiagnostic.newDiagnosticType(SRWDiagnostic.GeneralSystemError, e.getMessage()));
            return tl;
        }
        ScanResponseType scanResponse=null, newResponse=new ScanResponseType();
        TermType term, termArray[], tterm;
        TermsType terms;
        Vector termsV=new Vector();
        for(int i=0; i<mergeDB.length; i++) {
            if(mergeDB[i].scanResponse==null) {
                log.error("mergeDB["+i+"] ("+mergeDB[i].getName()+") returned: "+mergeDB[i].scanResponse);
                continue;
            }
            scanResponse=(ScanResponseType)mergeDB[i].scanResponse;
            diagnostics=scanResponse.getDiagnostics();
            if(diagnostics!=null) {
                if(newDiagnostics==null)
                    newDiagnostics=new DiagnosticsType();
                newDiagnostics.setDiagnostic(diagnostics.getDiagnostic());
            }
            terms=scanResponse.getTerms();
            if(terms==null) // no terms returned; probably got a diagnostic
                continue;
            termArray=terms.getTerm();
            vectorIndex=0;
            for(int j=0; j<termArray.length; j++) {
                term=termArray[j];
                if(vectorIndex>=termsV.size()) {
                    if(log.isDebugEnabled())
                        log.debug("adding to end: "+term.getValue());
                    termsV.add(term);
                    vectorIndex++;
                }
                else {
                    while(vectorIndex<termsV.size() &&
                      (comp=compare(termsV.get(vectorIndex), term))<0)
                        vectorIndex++;
                    if(vectorIndex>=termsV.size()) {
                        if(log.isDebugEnabled())
                            log.debug("adding to end: "+term.getValue());
                        termsV.add(term);
                        vectorIndex++;
                    }
                    else if(comp==0) {
                        tterm=(TermType)termsV.get(vectorIndex);
                        if(log.isDebugEnabled())
                            log.debug("changing count: "+tterm.getValue());
                        count=tterm.getNumberOfRecords().longValue()+term.getNumberOfRecords().longValue();
                        tterm.setNumberOfRecords(new NonNegativeInteger(Long.toString(count)));
                    }
                    else {
                        if(log.isDebugEnabled())
                            log.debug("inserting at "+vectorIndex+": "+term.getValue());
                        termsV.add(vectorIndex++, term);
                    }
                }
            }
        }
        if(maxTerms>=termsV.size()) { // just return what we have
            if(log.isDebugEnabled())
                log.debug("number of terms returned: "+termsV.size());
            termArray=new TermType[termsV.size()];
            termArray=(TermType[])termsV.toArray(termArray);
        }
        else { // pick the ones to return
            if(log.isDebugEnabled())
                log.debug("number of terms returned: "+termsV.size());
            for(bestFit=0; bestFit<termsV.size(); bestFit++)
                if(compare(scanTerm, (TermType)termsV.get(bestFit))<=0)
                    break;
            if(log.isDebugEnabled())
                log.debug("bestFit="+bestFit);
            int start=bestFit-position;
            if(start<0)
                start=0;
            if(start+maxTerms>termsV.size())
                maxTerms=termsV.size()-start;
            termArray=new TermType[maxTerms];
            for(int i=0; i<maxTerms; i++) {
                termArray[i]=(TermType)termsV.get(start++);
                if(log.isDebugEnabled())
                    log.debug("returning term: "+termArray[i].getValue());
            }
        }
        TermList tl=new TermList();
        tl.setTerms(termArray);
        if(newDiagnostics!=null) {
            if(log.isDebugEnabled())
                log.debug(newDiagnostics.getDiagnostic(0).getUri());
            tl.addDiagnostics(newDiagnostics);
        }
        if(log.isDebugEnabled())
            log.debug("scan "+scanTerm+": ("+(System.currentTimeMillis()-startTime)+"ms)");
        return tl;
    }


    public String getConfigInfo() {
        return mergeDB[0].getDB().getConfigInfo();
    }

    public String getExtraResponseData(QueryResult result, SearchRetrieveRequestType request) {
        return null;
    }
    
    public String getIndexInfo() {
        return mergeDB[0].getDB().getIndexInfo();
    }



    public QueryResult getQueryResult(String query,
      SearchRetrieveRequestType request) throws InstantiationException {
        long startTime=System.currentTimeMillis();
        rb.setRequest(request, ++counter);
        rb.waitUntilDone();
        MergedQueryResult result=new MergedQueryResult(mergeDB, rb);
        QueryResult qr;
        for(int i=0; i<mergeDB.length; i++) {
            if(mergeDB[i]==null)
                log.error("mergeDB["+i+"] is null!");
            else {
                qr=(QueryResult)mergeDB[i].result;
                if(qr==null)
                    log.error("mergeDB["+i+"].result is null!");
                else {
                    if(qr.hasDiagnostics())
                        result.addDiagnostics(qr.getDiagnostics()); // if there were any
                    result.setPartialResult(i, qr);
                }
            }
        }
        log.info("search "+query+": ("+(System.currentTimeMillis()-startTime)+"ms)");
        return result;
    }
    
    public String getSchemaInfo() {
        return mergeDB[0].getDB().getSchemaInfo();
    }


    public void init(final String dbname, String srwHome, String dbHome,
      String dbPropertiesFileName, Properties dbProperties) {
        log.info("entering init, dbname="+dbname);
        initDB(dbname, srwHome, dbHome, dbPropertiesFileName, dbProperties);
        
        String dbList=dbProperties.getProperty("DBList");
        if (dbList==null) {
            log.error("DBList not specified");
            log.error(".props filename is " + dbPropertiesFileName);
            return;
        }
        log.info("dbList="+dbList);
        
        StringTokenizer st=new StringTokenizer(dbList, ", \t");
        int numThreads=st.countTokens();
        rb=new RequestBucket(numThreads);
        mergeDB=new SRWDatabaseThread[numThreads];
        String dbName;
        for(int i=0; i<numThreads; i++) {
            dbName=st.nextToken();
            try {
                mergeDB[i]=new SRWDatabaseThread();
                mergeDB[i].init(rb, i, SRWDatabase.getDB(dbName, srwProperties));
                mergeDB[i].test("bogus");
                mergeDB[i].start();
            }
            catch(Exception e) {
                log.error(e);
            }
        }
        useSchemaInfo(getSchemaInfo());
        useConfigInfo(getConfigInfo());
        log.info("leaving init");
        return;
    }

    public boolean supportsSort() {
        return true;
    }
    
}
