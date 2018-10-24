#!/usr/bin/bash

process() 
{
	PARENT=$1
	FILE=$2
	TIME=`truss /usr/bin/date 2>&1 | grep ^time | awk -F" " '{print $3}'`
	echo $TIME
	
	TMPF="$AUDIT_HOME/tmp/$TIME.in"
	TMPF_1="$AUDIT_HOME/tmp/$TIME.out"
	cat $FILE | nawk -v S="$PARENT" '{ if ($0 ~ "ou=") {print "--name \""S""$0"\""; } }' > $TMPF
	/opt/tarantella/bin/tarantella object list_contents --file $TMPF > $TMPF_1
	
	TMPH="$AUDIT_HOME/tmp/$TIME.headers"
	cat $FILE | grep "ou=" | sed  s/"o=applications\/"//g > $TMPH
	
	TMPN="$AUDIT_HOME/tmp/$TIME.next"
	while read STU; do LL=`echo $STU | nawk '{ gsub(/\//, "\\\/"); print }'`; sed -n /o=applications\\\/"$LL":/,/Contents/p $TMPF_1 | grep -v Contents | grep -
	v "no contents" | nawk -v S="$STU" '{print "o=applications/"S"/"$0"";}' ; done < $TMPH > $TMPN
	
	TMPCN="$AUDIT_HOME/tmp/$TIME.cn"
	cat $TMPN | grep "cn=" | grep -v \? | awk '{print "--name \""$0"\""}' > $TMPCN
	#/opt/tarantella/bin/tarantella object list_attributes --file $TMPF >> all.tmp
	
	TMPOU="$AUDIT_HOME/tmp/$TIME.ou"
	cat $TMPN | grep -v "cn=" | uniq > $TMPOU
	
	if [ -s $TMPOU ]; then
	        $AUDIT_HOME/process.sh "" $TMPOU
	fi
}


OUTFILE="sgd.txt"
SOURCE="${BASH_SOURCE[0]}"
DIR="$( dirname "$SOURCE" )"
while [ -h "$SOURCE" ]
do
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
  DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd )"
done
H_HOME="$( cd -P "$( dirname "$SOURCE" )" && pwd )"; export H_HOME
source $H_HOME/../etc/agent.properties
pidfile=$H_HOME/../log/pid
host=`hostname`
AUDIT_HOME=$H_HOME../tmp/audit
if [ ! -x $AUDIT_HOME ]; then 
	mkdir $AUDIT_HOME
]

rm -f $AUDIT_HOME/tmp/*
rm -f $AUDIT_HOME/$OUTFILE*
PARENT="o=applications"
FILE="$AUDIT_HOME/tmp/s0.txt"

echo "Start"
/opt/tarantella/bin/tarantella object list_contents --name o=applications | grep -v Contents | grep -v "?" > $FILE

TIME=`truss /usr/bin/date 2>&1 | grep ^time | awk -F" " '{print $3}'`

TMPF="$AUDIT_HOME/tmp/$TIME.tmp"
echo $TMPF
cat $FILE | nawk -v S="$PARENT" '{ if ($0 ~ "cn=") {print "--name \""S"/"$0"\""; } }' > $TMPF
/opt/tarantella/bin/tarantella object list_attributes --file $TMPF >> $AUDIT_HOME/$OUTFILE

echo "Stating recursion"
#$AUDIT_HOME/process.sh "$PARENT/" $FILE
process "$PARENT/" $FILE
echo "Finished"

for T in `find $AUDIT_HOME -name "*.cn"`; do
        echo "Processing $T"
        /opt/tarantella/bin/tarantella object list_attributes --file $T >> $AUDIT_HOME/$OUTFILE
done

#$AUDIT_HOME/acgm SGDAudit $ACG_ENV < $AUDIT_HOME/$OUTFILE
