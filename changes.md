## GSN Change log

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
