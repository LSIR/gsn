#!/bin/sh

BACKLOG_PYTHON_DIR=/usr/lib/python2.6/backlog/

for i in $*
do
	if [ ! -f $i ]
	then
		echo $i does no exist!
		exit 1
	fi
done 

echo "uploading $*"

echo "to permasense-dh-bs01:"
scp $* root@permasense-dh-bs01:$BACKLOG_PYTHON_DIR

echo "to permasense-jj-bs01:"
scp $* root@permasense-jj-bs01:$BACKLOG_PYTHON_DIR

echo "to permasense-mh-bs01:"
scp $* root@permasense-mh-bs01:$BACKLOG_PYTHON_DIR

echo "to permasense-rd-bs01:"
scp $* root@permasense-rd-bs01:$BACKLOG_PYTHON_DIR

echo "to permasense-thur-bs01:"
scp $* root@permasense-thur-bs01:$BACKLOG_PYTHON_DIR

echo "to permasense-dh-gps01:"
scp $* root@permasense-dh-gps01:$BACKLOG_PYTHON_DIR

echo "to permasense-mh-cam01:"
scp $* root@permasense-mh-cam01:$BACKLOG_PYTHON_DIR

echo "finished uploading"