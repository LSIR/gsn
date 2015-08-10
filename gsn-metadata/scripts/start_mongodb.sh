#!/bin/bash

export LC_ALL=C
nohup mongod --dbpath /home/slf/metadata_db > ../logs/mongo.log &