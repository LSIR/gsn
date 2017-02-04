## GSN Change log

### r2.0.1
[commits](https://github.com/LSIR/gsn/commits/gsn-release-2.0.1)
* Adding support for MQTT (wrapper to subscribe and VS to publish)        
* Adding coap wrapper       
* Removing references to decommissioned osper server      
* Add reference to location fields 
* Fixing recovery from unsynchronized zmq communication with services      
* Fixing some configuration bugs

### r2.0.0
[commits](https://github.com/LSIR/gsn/commits/gsn-release-2.0.0)
* This is a major release and some of the changes below are NOT backward compatible.
* Splitting GSN into gsn-core, gsn-extra, gsn-services and gsn-webui.
* New web UI, based on the GSN API.
* Oauth2 provider (and consumer for Google login for example) and access control moved to the gsn-services
* Completely removing Jetty from gsn-core (removing API v1 and v2 and all servlets. The plan is to move the actually needed one to gsn-services). [more details](https://github.com/LSIR/gsn/commit/fea30806e3da30720204eed46b1d6b74f036034b)
* Adding websockets to the API for realtime streams (experimental feature).
* Migrating from ant/maven to sbt.
* Migrating all inter-GSN communication to zeroMQ/gsn-services API (zeromq-sync, zeromq-async, zeromq-push, remote-api wrappers).
* Adding vagrant for quick demo
* Migrating namespace from gsn to ch.epfl.gsn to publish artifacts on Sonatype.

### r1.1.8
[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.8)
* Adding monitoring of GSN internal metrics, output to munin and collectd
* Adding anomaly detection on streams (outliers, iqr, unique values)
* Adding the packager ant target for building Debian packages of GSN
* Fixing bug with postgresql (<9.2) and prepared transactions
* Making some small improvements of the web interface
* Adding json output for queries on Model Servlet
* Fixing wrapper resources released when initialization fails
* Fixing StreamElement duplication when having non-unique timestamps by using pk in sliding windows
* Switching to slf4j for logging (with default config using log4j2) + cleaning some log outputs
* Adding more export formats and functions to the API v3. (experimental)
* Updating the ZMQ protocol to match opensense deployment
* Adding support for partial ordering in wrappers (when having several sensors pushing to a single virtual sensor)
* Complete rewrite of tinyGSN, using singleton wrappers, queues and publisher. Adding subscription to GSN server and adaptive obfuscation of locations for privacy protection.

### r1.1.7
[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.7)
* Improved tinyGSN background application using AlarmManagers for saving the battery.
* Timestamp are not anymore shifted in /multidata response
* Adding possibility for Chart virtual sensor to specify windows length by time
* Adding maven support, automatic dependency management (only legacy libraries kept in /lib)
* Adding type bigint, mapping to long, to CSV parser in the Wrapper (as currently numeric maps to double). This can be used to directly input timestamps (in milliseconds)
* Allow for negative timestamps, they can be set before 1970.
* Updating jetty to support disabling of SSLv3 (not supported anymore by firefox 34)
* Keystore file location can be set in config file


### r1.1.6
[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.6)
* Major update on tinyGSN, virtual sensors are running as background services and a scheduler takes care of turning them on and off.
* Moved GSN sources to /src/main/java
* Updated licence to GPLv3 or later for compatibility with the libraries
* Added support for 32bit native floats as GSN types
* Adding support for pushing multiple stream elements in a single PUT request
* Updating plots on the webapp (using d3.js), new aggregators, sampling option and scatter plots


### r1.1.5
[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.5)
* Added possibility to omit latest values in request '/gsn?REQUEST=0'
* Added latest values in /sensors response (/service?latest_values=true)
* Added GeoJSON format for RestApi
* Updated commons codec, upgraded web xml 2.5
* Added the tinyGSN Android app, and google cloud messaging support
* Updated ContainerInfoHandler to support non-unique time-stamps


### r1.1.4
[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.4)
* Updated the logo and color scheme accordingly
* Added zeromq wrapper and distribution system
* Fixed GriddData out of memory
* Adding support for request 113 in REST API and some refactoring
* Filtering empty VS in multidata download


### r1.1.3
[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.3)
* Added support for HBase storage
* Updated REST API for accessing data
* Added access control support for REST PAI
* Added support for units in web services
* Added support for units in HTTP calls
* Added support for grid data
* Changing all absolute path to relative ones. This allows to run gsn behind a reverse proxy with any url.
* Fixed bug for the constructor of StreamElement taking another StreamElement as input.
* Updated weka library
* Added time limits for the access control
* Added notifications for the access control
* Modified loading order for Virtual Sensors (now in lexicographical order by their filenames)
* Added public getType and protected setData methods to StreamElements. Some private methods were also created to avoid code duplication.
* Added options to GridRenderer for adding a map overlay, setting the min/max values for the color scale and drawing the scale.
* Added the modelling virtual sensor and the related AbstractModel class (also with an example of implementation of a model)
* Added the GridModel virtual sensor
* Added the web interface to query the models
* Added the direct push remote wrapper, which extends the push remote wrapper for device that are not always connected. As the output structure as to be defined in the xml file, the structure of the virtual sensor is slightly modified (added a new optional field) and binding needs to be rebuilt. (JiBX bindings have issues with JRE7, consider using JRE6)
* Added Direct Remote Push virtual sensor sample
* Removed deprecation warnings and usage of sun.* API (http://www.oracle.com/technetwork/java/faq-sun-packages-142232.html)
* Moved duplicated code in gsn.http.ac.* classes to gsn.http.ac.UserUtils as static method
* Added Windows Phone Push service as a connectionless remote push wrapper. This class implements a generic push notification it should be adapted to the specific needs of each mobile application.
* Added continuous queries over models (merge from branch opensense)
* Avoid redirection to the login servlet when username and password are provided in the url for datagrid web service; Checking access rights for user in datagrid rest web service
* Ordered data latest on top by default in response for requests
* Updated csv file format for REST web service
* Included units in all server calls
* Fixed JiBx binding for compatibility with Java 1.7.x
* Added support for access control to grid servlet
* Added support for sub grids and time series generation from grid data
* Added Semantic Virtual Sensor for exporting data to LSM (http://lsm.deri.ie)


### r1.1.2

[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.2)
* New sensorscope listener server fixing problem of missing data
* Added count-based checkpoints to the CSV wrapper, logging line counts instead of latest timestamp
* Added new sensor types to sensorscope listener (NO2, CO, CO2, snow height, dendrometer, new temperature and air pressure sensors, data logger voltage, and GPS coordinates)
* Added /dynamicgeodata servlet, which allows making basic spatial queries for moving sensors
* Added new REST API for accessing data (beta)
* Fixed leak in unused connections (StorageManager pool configuration)

### r1.1.1

[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.1)
* Added URL-based queries for GML handler
* Added dataclean virtual sensor
* Added improved version of the jdbc wrapper
* Added grid renderer for rendering grid objects
* Added quality metric to data cleaning model
* Added web method for fetching grid objects
* Updated packet format for Sensorscope wrapper. Now supporting battery board 2.3, Decagon 10HS and Apogee SP-212
* Added support for PostGIS for geospatial queries
* Added simple web service client
* Added URL-based authentication to /GSN and /DATA requests
* Added group filtering to GML handler
* Fixed controller servlet. Allowing public data to be plotted and queried.
* Added non-parametric methods (ARMA GARCH) to data cleaning
* Reintroduced RSS wrapper
* Fixed bug in standard web server for null data

### r1.1.0

[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.1.0)
* Added Support for Flexible Storage Manager (processing/storage dbs, per virtual sensor db, in memory processing, transcient virtual sensor).
* Added Support for Access Control (alpha version, must not be used in production environment yet).
* Added Support for PostgreSQL as processing/storage db.
* Added Support for geospacial queries (see /geodata servlet).
* Added Support for GML output
* Added Support for periodical execution of code in the ScriptletProcessor.
* Added Support for SSL.
* Added A3DWebService Alpine3D Web Service.
* Added Wrapper that handles data grids (short name: grid).
* Added Timezone visualization per virtual sensor.
* Added Throughput performance evaluator and performance settings in build.xml
* Updated Microsoft SensorMap Web Service implementation to their latest API at sensormap.org.
* Fixed Missing duplicated stream elements with pagination.
* Fixed Multiple CSV wrappers reading from the same data file do not use the same checkpoint file anymore.
* Fixed Sorting the virtual sensor names and fields names in the web UI.
* Fixed the Virtual Earth js link.

### r1.0.1

[commits](https://github.com/LSIR/gsn/commits/gsn-release-1.0.1)
* Added the 'ScriptletProcessor' processing class which can be used to implement arbitrary complex processing class by specifying its logic in the Groovy language, directly in the virtual sensor description file.
* Added pagination support for MySQL (/data and /multidata servlet and DataDistributer) which enable GSN to stream an arbitrary large amount of data.
* Fixed OutOfMemory errors due to large amount of data requests.
* Fixed OutOfMemory errors due to a dbcp library memory leak when calling multiple time the getMetadata (This is going to be fixed in the dbcp 1.4.1).
* Fixed GSN hang forever due to a database connection leak in the (/data and /multidata servlets).
* Fixed synchronization bug related to jetty Continuation, in the remote-rest wrapper.
* Fixed reconnection bug in the remote-rest and remote-push wrappers.
* Fixed the org.apache.commons.dbcp.DelegatingPreparedStatement is closed Exception.
* Fixed exceptions thrown (such as: java.lang.NumberFormatException: multiple points) due to concurrent access to the /multidata servlet.
* Integrated the GSN installer in the trunk through the 'installer' ant task.
* Added the 'download_mode=inline' parameter to the /multidata servlet, in order to get the data not as an attachment.
* Removed the unused virtual-sensor pool, marked the 'lifeCyclePool' configuration element as deprecated.
