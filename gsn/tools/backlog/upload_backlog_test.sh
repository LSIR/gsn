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
echo "to permasense-etz-bs01:"
scp $* root@permasense-etz-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-bs02:"
scp $* root@permasense-etz-bs02:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-etz-bs03:"
scp $* root@permasense-etz-bs03:$BACKLOG_PYTHON_DIR

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
echo "to opensense-tram-bs02:"
scp $* root@opensense-tram-bs02:$BACKLOG_PYTHON_DIR

echo ""
echo "to root@opensense-due-bs01:"
scp $* root@opensense-caa-due-1.dyndns.biz:$BACKLOG_PYTHON_DIR

echo ""
echo "finished uploading"