# GSN Global Sensor Networks [![Build Status](https://travis-ci.org/LSIR/gsn.svg)](https://travis-ci.org/LSIR/gsn)

GSN is a software middleware designed to facilitate the deployment and programming of sensor networks. 

## Online Documentation

You can find the latest GSN documentation, including a deployment, installation, and programming
guide, on the project [wiki](https://github.com/LSIR/gsn/wiki).
This README file only contains basic setup instructions depending on your goal:

### Running and deploying GSN

#### Quick demo with Vagrant

On any computer that can run [VirtualBox](https://www.virtualbox.org/) (or any other supported virtual machine provider), install [Vagrant](https://www.vagrantup.com/), get the GSN git repository or just the file `Vagrantfile` and type `vagrant up` in your terminal (being in the same folder).
Once the provisioning is done, open your browser at http://localhost:8000/ to see the GSN UI. You can login with the username root@localhost and password changeme.


#### Debian package

To make it even easier to test on Linux or deploy at large scale, we provide debian packages (see releases). It includes a systemd script to start the GSN server modules automatically at boot and manage it like any other service. Configuration files are in `/etc/gsn-core/`, `/etc/gsn-services/` and `/etc/gsn-webui/`, the virtual sensors in `/etc/gsn-core/virtual-sensors/` and the logs can be found at `/var/log/gsn-core/`, `/var/log/gsn-services/` and `/var/log/gsn-webui/`. Starting and stopping GSN is performed with `service gsn-core start/stop`, `service gsn-services start/stop` and `service gsn-webui start/stop`. By default, the GSN web interface is then accessible at <http://localhost> and the API at <http://localhost:9000>, but you can change the ports in the configuration files.

#### Universal package

We provide a universal package for each release of the code. This package is the best way to easily try GSN features on non-Linux platforms. 

The installer binaries for the latest release can be found at:
<https://github.com/LSIR/gsn/releases>

Once GSN is installed, you can start it, executing the batch file `gsn-start.bat` (Windows) or shell script `gsn-start.sh` (Linux). 

#### Loading your first virtual sensor

To load a virtual sensor into GSN, you need to move its description file (.xml) into the `virtual-sensors` directory.
This directory contains a set of samples that can be used.

You can start by loading the MultiFormatTemperatureHandler virtual sensor (`virtual-sensors/samples/multiFormatSample.xml`).
This virtual sensor generates random values without the need of an actual physical sensor.

### Developing new wrappers or Virtual Sensors 

If you only need to write your own wrapper for a specific sensor communication protocol or processing class, you don't need to have the full building chain as in the next section. Just start an empty Java or Scala project and include a dependency to gsn-core (for example with maven):

```xml
<dependency>
    <groupId>ch.epfl.gsn</groupId>
    <artifactId>gsn-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

Then you can package your code as a jar and put it in the lib folder of the installer (after you followed the steps of the previous section) and you are ready to load you own wrapper or virtual sensor. In the case of a new wrapper you will also need to register it on the ``wrapper.properties`` file on your GSN installation.

### Building from sources

First download the code from the git repository (using ``--depth 1`` makes it a lot smaller if you don't need the 10 years history):

	git clone --depth 1 git@github.com:LSIR/gsn.git

The GSN modules have the following requirements for building from the sources:

* gsn-core and gsn-extra
  * sbt 0.13+
  * Java JDK 1.7
* gsn-tools and gsn-services
  * sbt 0.13+
  * Java JDK 1.7
  * Scala 2.11
* gsn-webui
  * python 3 
  * [bower](http://bower.io/)
  * [virtualenv](http://docs.python-guide.org/en/latest/dev/virtualenvs/)


Then you can run the following tasks in sbt:

* clean: remove generated files
* compile: compiles the modules
* package: build jar packages
* project [core|extra|tools|services|webui]: select a specific projet

In the project core you can use ``re-start`` to launch gsn-core for development and ``debian:packageBin`` to build the debian package.

In the project services you can use ``run`` to start the web api in development mode  and ``debian:packageBin`` to build the debian package.

In the project webui you can use ``startDjango`` to start the web interface in development mode  and ``packageDjango`` to build the debian package.

Never use the development mode commands to run a production server !!

