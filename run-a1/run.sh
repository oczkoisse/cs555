#!/usr/bin/env bash

while read invocation
do
    gnome-terminal -x bash -c "$invocation" &
done
