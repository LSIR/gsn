#!/bin/bash

export LC_ALL=C
nohup mongod --dbpath /home/slf/gsn/metadata_db > ../logs/mongo.log &