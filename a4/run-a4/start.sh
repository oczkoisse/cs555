#!/usr/bin/env bash

test_home=$HOME/Documents/Assignments/cs555/out/production/a4

gnome-terminal -x bash -c "ssh -t denver 'cd ${test_home}; java a4.nodes.controller.Controller 44000; bash;'" &
sleep 10

params=""
k=1
for i in $(grep '^[^#]' machine_list)
do
    echo "logging into ${i}"
    ((port = 44000 + k))
    ((k++))
    params="$params --tab -e \"ssh -t ${i} 'cd ${test_home}; java a4.nodes.server.Server ${port} denver 44000;bash;'\" -t ${i}"
done

cmd="gnome-terminal ${params}"
eval $cmd

gnome-terminal -x bash -c "ssh -t annapolis 'cd ${test_home}; java a4.nodes.client.Client 45000 denver 44000; bash;'" &
