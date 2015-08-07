#!/bin/bash

export LC_ALL=C
nohup mongod --dbpath /home/nataliya/data > ~../logs/mongo.log &