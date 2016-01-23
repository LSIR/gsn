# GSN Global Sensor Networks [![Build Status](https://travis-ci.org/LSIR/gsn.svg)](https://travis-ci.org/LSIR/gsn)

GSN is a software middleware designed to facilitate the deployment and programming of sensor networks. 

## Online Documentation

You can find the latest GSN documentation, including a deployment, installation, and programming
guide, on the project [wiki](https://github.com/LSIR/gsn/wiki).
This README file only contains basic setup instructions.

## Building

First download the code from the git repository (using ``--depth 1`` makes it a lot smaller if you don't need the 10 years history):

	git clone --depth 1 git@github.com:LSIR/gsn.git

GSN requires the following software for building:

* Jakarta apache ant version 1.7.x or higher.
* Java JDK 1.6.x .
* [Apache Maven](http://maven.apache.org/download.cgi)

To build GSN follow these steps:
* Add ANT_HOME/bin folder to your PATH
* Execute ant with the build task:
	``ant build``

To run GSN from the source code, you can run the following ant task:
	``ant gsn``

To stop GSN:
	``ant stop``

Starting from version 1.1.7 GSN is using apache maven for managing the dependencies and libraries.
If you use a IDE such as eclipse or NetBeans you may need to install additional plugins and change the settings of the project:

* For eclipse: 
  * You can install [M2Eclipse](http://eclipse.org/m2e/) 
  * Right-click on your GSN project -> configure -> Convert to Maven project... 
  * Right-click on your GSN project -> Build Path -> Configure build path...
  * On the libraries tab, remove all references to the libraries that are marked in red.
  * Press alt+F5 and update the maven libraries (it may take a while the first time) 
  * If it still complains about missing libraries, check if they are in the lib folder and if needed add them to the build path

## Download Installer

We provide a multiplatform GSN Installer for the last release of the code. This installer is the best way to easily try GSN features. 

The installer binaries for the latest realease can be found at:
<https://github.com/LSIR/gsn/releases>

Once GSN is installed, you can start it, executing the batch file `gsn-start.bat` (Windows) or shell script `gsn-start.sh` (Linux). 

The GSN web interface is accessible at <http://localhost:22001>

## Loading your first virtual sensor

To load a virtual sensor into GSN, you need to move its description file (.xml) into the `virtual-sensors` directory.
This directory contains a set of samples that can be used.

You can start by loading the MultiFormatTemperatureHandler virtual sensor (`virtual-sensors/samples/multiFormatSample.xml`).
This virtual sensor generates random values without the need of an actual physical sensor.

Virtual sensors are visible in the GSN web interface: <http://localhost:22001>


