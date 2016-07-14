# GSN Global Sensor Networks : the Web UI

This is a web interface for exploring the data processed by GSN. It is based on the python Django framework and AngulaJS. It interacts with the Services module.

## Quick start

Before starting you need python3 and [bower](http://bower.io/). It is also recommended to work inside a [virtualenv](http://docs.python-guide.org/en/latest/dev/virtualenvs/).

    pip install -r requirements.txt
    cp app/settingsLocal.py.dist app/settingsLocal.py
    python manage.py migrate
    python manage.py bower_install
    python manage.py runserver 

## Configuration

You can setup the backend database used by Django for storing users preferences by editing the app/settingsLocal.py file. It also contains the informations to connect to the GSN server API.

For production environments, don't use the integrated web server and refer to the official [Django documentation](https://docs.djangoproject.com/en/1.8/howto/deployment/) or use a packaged release of gsn-webui.


