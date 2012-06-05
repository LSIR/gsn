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
echo "to permasense-dh-bs01:"
scp $* root@permasense-dh-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-dh-gps01:"
scp $* root@permasense-dh-gps01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-dh-gps02:"
scp $* root@permasense-dh-gps02:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-dh-gps03:"
scp $* root@permasense-dh-gps03:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-gg-bs01:"
scp $* root@permasense-gg-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-gg-gps01:"
scp $* root@permasense-gg-gps01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-jj-bs01:"
scp $* root@permasense-jj-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-mh-bs01:"
scp $* root@permasense-mh-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-mh-cam01:"
scp $* root@permasense-mh-cam01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-rd-bs01:"
scp $* root@permasense-rd-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-rd-cam02:"
scp $* root@permasense-rd-cam02:$BACKLOG_PYTHON_DIR

echo ""
echo "to permasense-thur-bs01:"
scp $* root@permasense-thur-bs01:$BACKLOG_PYTHON_DIR

echo ""
echo "to opensense-caa-tram-2:"
scp $* root@opensense-caa-tram-2.dyndns.biz:$BACKLOG_PYTHON_DIR

echo ""
echo "to opensense-caa-tram-3:"
scp $* root@opensense-caa-tram-3.dyndns.biz:$BACKLOG_PYTHON_DIR

echo ""
echo "to opensense-caa-tram-4:"
scp $* root@opensense-caa-tram-4.dyndns.biz:$BACKLOG_PYTHON_DIR

echo ""
echo "to opensense-caa-tram-5:"
scp $* root@opensense-caa-tram-5.dyndns.biz:$BACKLOG_PYTHON_DIR

echo ""
echo "to opensense-caa-tram-6:"
scp $* root@opensense-caa-tram-6.dyndns.biz:$BACKLOG_PYTHON_DIR


echo ""
echo "finished uploading"