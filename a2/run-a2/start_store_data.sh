#!/usr/bin/env bash

test_home=$HOME/Documents/Assignments/cs555/out/production/a2

gnome-terminal --working-directory "$test_home" --window -e "java a2.nodes.storer.StoreData 45000 denver 44000"
