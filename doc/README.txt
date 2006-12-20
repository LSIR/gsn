
Thank you for downloading GSN!

There are a few things good to know in order to get the most
from GSN.

Licensing terms
===============
GSN is licensed under the GNU GPL license, version 2. A complete copy
of the text can be found in the file GPL-2.txt.
Basically, it grants you the rights to use, redistribute and modify
GSN. However, if you want to redistribute a modified version of GSN,
you must provide the complete source code of your modifications.

What is GSN
===========

GSN is a software platform designed to efficiently receive
data from various data sources. It can forward these data
streams to small Java programs called Virtual Sensors.

Virtual sensors receive data from one or more input streams,
perform operations on these streams and generate other
data streams, which can again be sent to other virtual sensors.

A virtual sensor can get its data from another virtual sensor
running on a separate GSN server.

Data can be sent to various output devices. This is called
the notification subsystem. For example, a virtual sensor
can send data to an sms gateway.


How to use GSN
==============

GSN ships with a graphical configuration tool. You can start
it with the following command:

Windows				gsn-gui.bat
Mac OS X, Linux		gsn-gui.sh

For more advanced configurations, like servers without graphical
display, you can use the "nogui" script files to start gsn in
text mode.


GSN uses two processes: the directory service, and gsn itself.
GSN needs a directory service to run. The default settings from
the graphical interface should be fine. Simply click on the green
button to start the service. After some initialization time, the
button should show a red square icon to allow you to stop the
service. If it does not, please check the log output display
from the graphical interface (Click on Directory Server log).
You can test the service by visiting

http://localhost:1882

with your web browser.

Once the directory server is running, you can start GSN. Click on
the start button to run it. Again, after some time the button
should display a red square box.
The log output (in "GSN Log") should start displaying lots of
texts. To reduce the amount of text, change the log level from
INFO to WARN. To stop the log window scrolling, uncheck the box
"Scroll log window when new data is available". You may find
this feature useful to inspect warnings or eventual errors.

If GSN seems to be running successfully, please visit

http://localhost:22001

There you can see the output of various virtual sensors that
are enabled by default with GSN.


How to use GoogleMaps with GSN
==============================

GoogleMaps is a service offered free of charge by Google.
To use it, you need to register at 
http://www.google.com/apis/maps/signup.html

You will receive a key. You should edit the 
"webapp/fullmap.html" and "webapp/index.html" files, and replace 

key=ABQIAAAA2MV0HBY9S6-aGXtjN0b1ZxTNHpjHnpWHHgdGqnMBUpwQOS9hiRQYia2Wq5RjKhXafOPvhLZP4RAQOA

with the key value you have received.


For more information, don't hesitate to contact us !



The GSN Development Team.


