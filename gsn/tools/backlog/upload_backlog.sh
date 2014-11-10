#!/bin/bash

BACKLOG_PYTHON_DIR=/usr/lib/python2.6/backlog
RCS_KEYWORDS_SCRIPT=rcs-keywords

PRODUCTIVE_HOSTS=(
	permasense-adm-bs01
	permasense-dh-bs01
	permasense-dh-gps03
	permasense-gg-bs01
	permasense-gg-cam01
	permasense-gg-cam02
	permasense-gg-gps01
	permasense-hs-bs01
	permasense-hs-cam01
	permasense-jj-bs01
	permasense-mh-bs01
	permasense-mh-cam01
	permasense-rd-bs01
)
TEST_HOSTS=(
	permasense-etz-bs01
	permasense-etz-bs02
	permasense-etzl-bs01
)

USAGE="Usage: gsn.sh {productive|test|HOST} FILES"

if [ $# -lt 2 ]
then
  echo $USAGE
  exit 1
fi

case "$1" in
  productive)
  	HOST=${PRODUCTIVE_HOSTS[@]}
    ;;
  test)
  	HOST=${TEST_HOSTS[@]}
    ;;
  *)
  	HOST=($1)
    ;;
esac

echo "uploading..."

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
tmpFolder="tmp$RANDOM"
mkdir -p $tmpFolder

shift
while [ $# -ne 0 ]; do
	cat $1 | "$dir/$RCS_KEYWORDS_SCRIPT" "$dir/python/$1" > $tmpFolder/$1
	shift
done

for h in $HOST
do
	echo "uploading to $h"
	scp $tmpFolder/* root@$h:$BACKLOG_PYTHON_DIR
done

rm -r $tmpFolder

echo "finished uploading"