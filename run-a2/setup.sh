#!/bin/bash

test_home=$HOME/Documents/Assignments/cs555/out/production/cs555

k=1
for i in `cat machine_list`
do
    echo "logging into ${i}"
    ((port = 44000 + k))
    ((k++))
    echo gnome-terminal -x bash -c "ssh -t ${i} 'cd ${test_home}; java cs555.a2.nodes.client.Client ${port} denver 44000 300;bash;'"
done