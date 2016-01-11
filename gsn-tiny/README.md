# TinyGSN (part of Global Sensor Networks)

TinyGSN is a lighter version of the software middleware GSN developped for android. 

## Online Documentation

You can find the latest GSN documentation, including a deployment, installation, and programming
guide, on the project [wiki](https://github.com/LSIR/gsn/wiki).
This README file only contains some information about the Android app.

## Description

This scaled-down version of GSN, which will be called tinyGSN, will open the door to radically new applications not possible with current server-based setup. These applications will be built through a combination of small software bricks following the GSN philosophy of little-or-zero-programming. One can imagine using his own smartphone during his morning jogging and have tinyGSN record his path using the internal GPS and his steps using a connected Pedometer. This data can later be visualized on the smartphone screen with statistics, progress, number of steps, distance, etc. We can also envision healthcare applications, where a blood pressure meter can be connected to a digital tablet and vital parameters recorded and sent regularly to Hospital. We can even easily send alerts in case of abnormal measurements. Another interesting application will come from pushing data to a regular GSN server. In this case, data from multiple devices can be aggregated for more complex applications like “crowd-sensing”. In a social sensing project, we can, imagine a community of citizens who participate in a collective effort for monitoring air pollution in a city for example. 

Demo: http://youtu.be/YdQL2FmwEIM

## Building

First download the code from the main GSN git repository and create an Android project from the ./gsn-tiny folder:

    git clone git@github.com:LSIR/gsn.git

TinyGSN requires the following for building:

* Android API 4.0.3 or higher.
* http://actionbarsherlock.com/ setup as a dependent library project (in eclipse)

TinyGSN uses also a location privacy library which is present as a git submodule. When you clone the repository, you need to init and pull the library using these commands :

    git submodule init
    git submodule update

## Download Package

We will provide a compiled package for installing directly on Android devices with each GSN release, starting from version 1.1.5. 

The installer binaries for the latest realease can be found at:
<https://github.com/LSIR/gsn/releases>
