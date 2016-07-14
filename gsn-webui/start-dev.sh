#!/bin/bash
cd gsn-webui
[ -d env3 ] || virtualenv -p python3 env3
source env3/bin/activate
pip install -r requirements.txt
python manage.py bower install
python manage.py migrate
python manage.py runserver
deactivate
cd ..
