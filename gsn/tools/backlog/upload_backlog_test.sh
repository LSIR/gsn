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

echo ""
echo "to permasense-etzg-bs01:"
scp $* root@permasense-etzg-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etzl-bs01:"
scp $* root@permasense-etzl-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-gps01:"
scp $* root@permasense-etz-gps01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-gps02:"
scp $* root@permasense-etz-gps02:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-gps03:"
scp $* root@permasense-etz-gps03:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-gps04:"
scp $* root@permasense-etz-gps04:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-gps05:"
scp $* root@permasense-etz-gps05:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-gps06:"
scp $* root@permasense-etz-gps06:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-cam01:"
scp $* root@permasense-etz-cam01:$BACKLOG_PYTHON_DIR

echo ""
echo "to pc-5225:"
scp $* tgsell@pc-5225:/home/tgsell/backlog/python/

echo ""
echo "finished uploading"