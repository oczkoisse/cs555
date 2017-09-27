#!/usr/bin/env bash

test_home=$HOME/Documents/Assignments/cs555/out/production/cs555

gnome-terminal -x bash -c "ssh -t denver 'cd ${test_home}; java cs555.a2.nodes.discoverer.Discoverer 44000; bash;'" &
sleep 5

params=""
k=1
for i in $(grep '^[^#]' machine_list)
do
    echo "logging into ${i}"
    ((port = 44000 + k))
    ((k++))
    params="$params --tab -e \"ssh -t ${i} 'cd ${test_home}; java cs555.a2.nodes.client.Client ${port} denver 44000 300;bash;'\" -t ${i}"
done

cmd="gnome-terminal ${params}"
eval $cmd
