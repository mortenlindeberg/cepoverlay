#!/bin/bash

for d in `find ./run_1 -d -maxdepth 1`
do
  for r in run_1 run_2 run_3 run_4 run_5
  do
    c=$(basename $d)
    a1=$(cat $r/$c/A1/app.log|grep $1 |wc -l)
    b1=$(cat $r/$c/B1/app.log|grep $1 |wc -l)
    e1=$(cat $r/$c/E1/app.log|grep $1 |wc -l)
    e2=$(cat $r/$c/E2/app.log|grep $1 |wc -l)
    i1=$(cat $r/$c/I1/app.log|grep $1 |wc -l)
    d1=$(cat $r/$c/D1/app.log|grep $1 |wc -l)

    echo $r/$c/A1 a1: $a1, b1: $b1, e1: $e1, e2: $e2, i1: $i1, d1: $d1 
  done
done
