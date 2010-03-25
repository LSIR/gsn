#!/bin/sh
if [ $# -lt 2 ]
then
	echo 'usage generatePrivateVSFromTemplate.sh <deployment template> <new deployment name>'
	echo
	echo 'e.g. "generatePrivateVSFromTemplate.sh Matterhorn Thur" to generate Thur deployment VS from Matterhorn template.'
else
	# generate
	#	all dozer VS
	# 	psbacklogstatus
	#	basestation
	for i in `ls $1_*.xml | sed '/_pos[0-9]\+_/d'`
	do
		if [ \
		 `sed '/gsn\.wrappers\.backlog\.plugins\./!d' $i | wc -l` -eq 1\
		-o `sed '/wrapper="remote-rest"/!d' $i | wc -l` -eq 1\
		-o `sed '/wrapper="local"/!d' $i | wc -l` -eq 1\
		]
		then
			echo $i
			destname=`echo $i | sed "s/$1/$2/"`
			if [ -e $destname ]
			then
				echo $destname exists, no changes made.
			else
				sed "s/$1/$2/" $i > $destname
			fi
		fi		
	done
fi
