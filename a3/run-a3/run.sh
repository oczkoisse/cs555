#!/usr/bin/env bash

function print_usage
{
    echo "USAGE: run.sh <name_of_job> [test|sample|full] (default test)"
}

if [ "$1" = "" ];
then
    print_usage
    exit 0
else
    case "$2" in
        "sample")
            target="/data/sampleData"
            ;;
        "" | "test")
            target="/home/testData"
            ;;
        "full")
            target="/data/fullData"
            ;;
        *)
            print_usage
            exit 0
            ;;
    esac

    job=${1##*.}
    scp ../../out/artifacts/a3_jar/a3.jar blue.cs.colostate.edu:/tmp/a3_bangar91/a3.jar
    params="export HADOOP_CONF_DIR=\$HOME/local/hadoop/client_conf ; \$HADOOP_HOME/bin/hdfs dfs -rm -r -f /home/out/$job /home/inter; \$HADOOP_HOME/bin/hadoop jar /tmp/a3_bangar91/a3.jar "$1" "$target" /home/out/$job; \$HADOOP_HOME/bin/hdfs dfs -cat /home/out/$job/part-r-00000 | less; bash"
    echo "$params"
    gnome-terminal --window -- ssh -t blue.cs.colostate.edu "$params"
fi
