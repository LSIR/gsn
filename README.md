# GSN Global Sensor Networks

GSN is a software middleware designed to facilitate the deployment and programming of sensor networks. 

## Online Documentation

You can find the latest GSN documentation, including a deployment, installation, and programming
guide, on the project [wiki](https://github.com/LSIR/gsn/wiki).
This README file only contains basic setup instructions.

## Building

First download the code from the git repository:

	git clone git@github.com:LSIR/gsn.git

GSN requires the following software for building:

* Jakarta apache ant version 1.7.x or higher.
* Java JDK 1.6.x .

To build GSN follow these steps:
* Add ANT_HOME/bin folder to your PATH
* Execute ant with the build task:
	``ant build``

To run GSN from the source code, you can run the following ant task:
	``ant gsn``

To stop GSN:
	``ant stop``

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


