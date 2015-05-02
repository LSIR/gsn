1. Fuseki infrastructure 

1.1 Install Apache-Jena-Fuseki SPARQL server
  - Download the application from http://jena.apache.org/documentation/serving_data/#download-fuseki1
  - Unpack it in a folder
  - Launch the application with the command ./fuseki-server from within the application folder

1.2 Administration page: http://localhost:3030

1.2.1 The data are stored in 3 datasets:
  - properties: contains properties definitions (typically initialized with cf-property.owl 
  and slf-property.owl using "add data" button on the home page)
  - mappings: contains the mappings between one property (attribute name in the database for example) and its 
  standardized name (the so called observed property)
  - virtual-sensors: contains the association between each virtual sensor and its outputs. 

1.2.2 To create the datasets:
  - go to "manage datasets", then "add new dataset"
  - enter the name of the dataset in "Dataset name"
  - choose the radio button "Persistent"
  - click "create dataset"

2. gsn-sensor-data-discovery tool:

2.1 This tool allows to:
  - Add new mappings to Fuseki based on a CSV file that has 2 columns: the first is the property name and the second 
  is the observed property URI
  - Add new virtual sensors from JSON or XML files (one sensor definition per file)

2.2 Configuration    
  Its configuration file is gsn/gsn-tools/gsn-sensor-data-discovery/src/main/resources/application.conf
  There are two things that need to be set:
    - Fuseki endpoints (URLs for querying datasets) 
    - Base URI: .... explications
      for example: http://slf.ch# 
      Note: it needs to end with #

2.3 Running the tool:
  - go to gsn/gsn-tools
  - type "sbt"
  - type "project gsnsensordatadiscovery"
  - type "run args" 
  where args is either:
    "--add-new-virtual-sensors virtualSensorsFolderPath" that will
    read every JSON and XML virtual sensor definition files in virtualSensorsFolderPath and add them to Fuseki
    Note: Every properties of a sensor that don't have any mapping in Fuseki (dataset "mappings") will be ignored 
    (the sensor will not have these properties as outputs)
  
    or "--add-new-mappings csvFilePath" that will read the provided CSV file and insert mappings into Fuseki
    The CSV file must have two columns: the first one is the non standard properties and the second is the URI of the
    observed properties found in the Fuseki "properties" model (typically filled with cf-property.owl 
    and slf-property.owl)
    Note: Every observed properties URIs (2nd column of the file) must exist in Fuseki (dataset "properties") or will
    be ignored 
  









