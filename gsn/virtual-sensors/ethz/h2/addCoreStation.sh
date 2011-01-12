#!/bin/sh
# usage: addCoreStation.sh templatehostname newhostname
if [ ! $# -eq 2 ]
then
  echo usage: addCoreStation.sh templatehostname newhostname
  exit 1
fi

for i in `grep "$1" * | sed 's/:.*//;/~/d'`
do
  echo $i
  # last data no.
  data_id=`sed -n '1h;1!H;${g;s/\(.*<\/stream>\s*\|.*<streams>\s*\)\(<stream.*<\/stream>\)\(\s*<\/streams>.*\)/\2/;s/.stream\s*name="data\([0-9]*\)/\1/;s/".*//;p}' $i`
  if [ `echo $data_id | wc --words` -eq 1 ]
  then
    data_id=$(($data_id+1))
  else
    data_id=0
  fi
  sed -n -i '1h;1!H;${g;s/\(.*<\/stream>\).*/\1/;p;x;s/\(.*<\/stream>\|.*<streams>\)\(\s*<stream.*'$1'.*<\/stream>\)\(.*\)/\2\3/;s/\(<\/stream>\s*\).*\(\s*<\/streams>.*\)/\1\2/;s/'$1'/'$2'/;s/<stream name="\([^"]*\)">/<stream name="data'$data_id'">/;p}' $i
done

exit 0

