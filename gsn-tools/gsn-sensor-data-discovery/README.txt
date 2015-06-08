1. Fuseki infrastructure 

1.1 Install Apache-Jena-Fuseki SPARQL server
- Download the application from http://jena.apache.org/documentation/serving_data/#download-fuseki1
- Unpack it in a folder
- Launch the application with the command ./fuseki-server from within the application folder

1.2 Administration page: http://localhost:3030

1.2.1 The data are stored in 3 datasets:
- properties: contains properties definitions (typically initialized with cf-property.owl 
and slf-property.owl using "add data" button on the home page)
- mappings: contains relationships between non obviously related resources
for example, a resource "output" has a database field and an observed property.
This output is then linked to virtual sensors
- virtual-sensors: contains the virtual sensors metadata 

1.2.2 To create the datasets:
- go to "manage datasets", then "add new dataset"
- enter the name of the dataset in "Dataset name"
- choose the radio button "Persistent"
- click "create dataset"


2. gsn-sensor-data-discovery
This project includes 2 tools, see section 2.1 and 2.2 for details
It is also a dependence of the gsn-tools Scala project (see its build.sbt file)

2.1 MetadataCreator Tool:

2.1.1 This tool allows to:
- Add new mappings to Fuseki based on a CSV file that has 2 columns: 
 - the first is the property name 
 - and the second is the observed property URI (or label but URI is should be preferred because unique)
- Add new virtual sensors from JSON or XML files (one sensor definition per file)

2.1.2 Configuration    
Its configuration file is gsn/gsn-tools/gsn-sensor-data-discovery/src/main/resources/application.conf
There are two things that need to be set:
- Fuseki endpoints (URLs for querying datasets) 
- Base URI: Base URI for the home made resources such as virtual sensors
for example: http://slf.ch# 
Note: it needs to end with #

2.1.3 Running the tool:
- go to gsn/gsn-tools
- type "sbt"
- type "project gsnsensordatadiscovery"
- type "run args" 
where args is either:
- "--add-new-virtual-sensors virtualSensorsFolderPath" 
 This will read every JSON and XML virtual sensor definition files in virtualSensorsFolderPath and add them to Fuseki
 
 or 
    
- "--add-new-property-mappings csvFilePath" that will read the provided CSV file and insert mappings into Fuseki
 The CSV file must have two columns: the first one is the virtual sensor field name and the second is the URI of the
 observed properties found in the Fuseki "properties" dataset (typically filled with cf-property.owl 
 and slf-property.owl)
 Note: Every observed properties URIs (2nd column of the file) must exist in Fuseki (dataset "properties") 
 or the mapping will be ignored

2.2 Discovery Agent (DA)
2.2.1 Introduction
DA is an application supposed to run as a kind of daemon: 
It's main features are:
- Send data discovery request (DDR) to DAs of cooperating systems
- Answer to DDRs with data discovery answers (DDA)

2.2.2 Operation

2.2.2.1 Sending DDRs
A DDR has the following parameters:
- A request id: used to group the answers
- URI of an observed property
- [Optional] rectangular location (2 points: (x1,y1) and (x2,y2))
- [Optional] altitude range (2 values: lower and upper bound)

The receiving process of DDRs is fully automatic as long as the agent is running
The sending process of DDRs is automatic once the DDR parameters have been defined
In this prototype, the DDRs are hard coded in XML files and are executed when the agent starts

2.2.2.2 Receiving DDAs
A DDA has the following content:
- The request id of the corresponding DDR
- The id of the GSN instance that issued the answer
- The observed property of the original DDR
  and a list of virtual sensor results (VSR) which contains:
- gsnId: identify the GSN instance the resulting virtual sensor belongs to 
- host: the URL that will be used in the remote wrapper to establish the data connection feed
- port: port for the remote wrapper
- vsName: name of the source virtual sensor
- fieldName: name of the virtual sensor field that contains the relevant data
- [Optional] longitude: longitude of the virtual sensor
- [Optional] latitude: latitude of the virtual sensor
- [Optional] altitude: altitude of the virtual sensor


A DA receives one data discovery answer per contacted external system 
Because all the results are supposed to be merged in one resulting virtual sensor,
the DA needs to wait for all answers. To avoid infinite waiting, a timer is set for the DDAs,
and the DA starts the processing, ignoring the missing ones.

The DA creates the virtual sensor definition file corresponding to the new virtual sensor that aggregates
all the ones returned in the results.
It creates also the necessary metadata in Fuseki
 

2.2.3 Configuration
The configuration file is src/main/resources/application.conf (same as for MetadataCreatorTool)

Relevant properties of this file for the Discovery Agent are:
- gsnId: identifier of the GSN instance this DA operates for
- host: fully qualified domain name of this GSN instance that the other instances will use in their remote wrappers
- port: port of this GSN instance, also for remote wrappers
- ddrFolder: path of the folder containing the data discovery request XML files

2.2.4 Improvements needed
This is a prototype version and needs improvements to be fully functional: see following subsections

2.2.4.1 History of events
It needs to have some kind of history of the requests made in order to be able
to discriminate between remote data sources that it knows already about and the new ones
It could also record the relationship between all the remote source virtual sensors and the resulting one.
This would allow to know the exact location of every sources instead of the average of all of them which might not make sense
in case the location defined is big  

2.2.4.2 Miscellaneous
- Generate automatically unique identifier for the DDR (using the history for example)
  it is used to make the resulting virtual sensor name unique
- Find a way to send the unit and type of the data along with each results (attributes of elements "field" in the VSD files)
  ideally, it wouldn't come from the VSD but from the metadata stored on Fuseki
  Then, proper unit conversions would be needed on each feed to maintain coherence (e.g. meters, centimeters, millimeters, ... => meters)

2.2.4.3 User interface
An actual user interface would be much better (and user friendly) than XML files
It could be used to create and send new DDR, see results, add new collaborating instances, ...

2.2.4.4 Reliability
Currently, if something fails, there is no retry, no logging, ...
Could add fault tolerance features










