#!/usr/bin/env bash

head -1 run | bash run.sh &
sleep 2
tail -n +2 run | bash run.sh &
