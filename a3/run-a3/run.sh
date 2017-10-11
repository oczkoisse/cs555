#!/usr/bin/env bash

if [ "$1" = "" ];
then
    echo "USAGE: run.sh <name_of_job>"
else
    job=${1##*.}
    scp ../../out/artifacts/a3_jar/a3.jar blue.cs.colostate.edu:/tmp/a3_bangar91/a3.jar
    params="export HADOOP_CONF_DIR=\$HOME/local/hadoop/client_conf ; \$HADOOP_HOME/bin/hdfs dfs -rm -r -f /home/out/$job; \$HADOOP_HOME/bin/hadoop jar /tmp/a3_bangar91/a3.jar /data/sampleData /home/out/$job; bash"
    echo "$params"
    gnome-terminal --window -- ssh -t blue.cs.colostate.edu "$params"
fi
