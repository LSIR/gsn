
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

For any question regarding the project please refer to the mailing
lists (see http://sourceforge.net/mail/?group_id=158046).
We strongly suggest users to register to the mailing lists as it is
the main source for development and feature news. It also serves as
the primary medium for discussions on development, deployment issues, etc.


How to use GSN
==============
GSN ships with a commandline tools. You can start
it with the following command:

Windows				gsn-no-gui.bat
Mac OS X, Linux		gsn-no-gui.sh

After some initialization time, you'll see several
log messages, now you can test the service by visiting

http://localhost:22001

with your web browser.

Note that, the log output (in "GSN Log") normally displays lots of
texts, which is normal. To reduce the amount of text, change the log level from
INFO to WARN (in the conf/log4j.properties (See the book of GSN file in doc folder).


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


