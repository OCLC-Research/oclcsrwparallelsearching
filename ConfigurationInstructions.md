# Implementing an SRWParallelSearchingDatabase #

You need to let the SRW server know of the existence of your Federated searching database.  You do this by adding three lines to the SRWServer.props file.  This file is typically found in your webapps/SRW/WEB-INF/classes directory.  If it is not there, or has a different name, then the webapps/SRW/web.xml file will have an explicit pointer to it.

Add these three lines to the SRWServer.props file:
```
  db.<databaseName>.class=ORG.oclc.os.SRW.ParallelSearching.SRWMergeDatabase.class
  db.<databaseName>.home=<full pathname to the directory with your database configuration file>
  db.<databaseName>.configuration=SRWDatabase.props
```

The `db.<databaseName>.home` property points to the directory that contains the database configuration file.  This is also where SRW will expect to find any other files necessary for its operation.

The `db.<databaseName>.configuration` property is optional.  If the database home directory was specified then the SRW server will automatically look for a file names SRWDatabase.props in that directory.  Otherwise, provide the name of the configuration file.

An example might be:
```
db.FederatedDSpace.class=ORG.oclc.os.SRW.ParallelSearching.SRWMergeDatabase.class
db.FederatedDSpace.home=f:/DSpace/config/
db.FederatedDSpace.configuration=SRWDatabase.FederatedSearching.props
```

The databases to be searched must be local SRW databases.  Another SRWDatabase implementation provides access to remote databases as local databases.  To do this, you add the same sort of information as above, but with a different database class.

An example might be:
```
db.RemoteMITDSpace.class=ORG.oclc.os.SRW.ParallelSearching.SRWRemoteDatabase.class
db.RemoteMITDSpace.home=f:/DSpace/config/
db.RemoteMITDSpace.configuration=SRWDatabase.MITDSpace.props
```

# Database Specific Configuration Information #
The SRWDatabase.props file serves two primary roles.  First, it contains much of the information needed to generate an Explain record for the database.  Second, it specifies classes and configuration information necessary to generate an SRW gateway to a local database system.

## General Database Information ##
|databaseInfo.title (recommended) | The name of the database.|
|:--------------------------------|:-------------------------|
|databaseInfo.description (optional) | A brief description of the database.|
|databaseInfo.author (optional) | The author/creator of the database.|
|databaseInfo.contact (recommended) | A person to contact with questions/problems.|
|databaseInfo.restrictions (optional) | Any usage restrictions.|


## Information about the Explain record ##
|metaInfo.dateModified (optional) | The date the record was last modified.|
|:--------------------------------|:--------------------------------------|
|metaInfo.aggregatedFrom (optional) | If the record was collected from another site, the URL of the original record.|
|metaInfo.dateAggregated (optional) | If the record was collected , the date the record was collected.|


## Default configuration values ##
|configInfo.maximumRecords (optional) | The maximum number of records that can be returned in a response.  Default is 20.|
|:------------------------------------|:---------------------------------------------------------------------------------|
|configInfo.numberOfRecords (optional) | The number of records to return in a response if not specified in the request.  Default is 10.|
|configInfo.resultSetTTL (optional) | The number of seconds that query results should be kept, if not specified in the request.  Default is 300.|


## Information about the record schemas supported and how to generate records in those schemas ##
All other information needed for the Explain record is copied from the Explain record of the first database in the DBList (see below).  This includes available schemas and indexes.

## Federated Searching Specific Information ##
|DBList| A comma, space, or tab separated list of local SRU database names.|
|:-----|:------------------------------------------------------------------|

A separate SRWDatabase interface is provided to support remote SRU databases as if they were local databases.

# Example SRWDatabase.props file #
Here is my SRWDatabase.props file for the Federated Searching Test Database:
```
databaseInfo.title=Federated Searching Test Database
databaseInfo.description=A database federating 2 copies of the builtin test database
databaseInfo.contact=Ralph LeVan (levan@oclc.org)

DBList=test, test
```

## Remote Searching Specific Information ##
|remoteURL| The URL of the remote SRU database.|
|:--------|:-----------------------------------|

# Example SRWDatabase.props file #
```
databaseInfo.title=OCLC's Identities Database
databaseInfo.description=OCLC's Identities Database
databaseInfo.contact=Ralph LeVan (levan@oclc.org)

remoteURL=http://worldcat.org/identities/srw/search/Identities
```