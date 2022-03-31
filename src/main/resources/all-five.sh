#!/bin/bash

for d in `find ./run_1 -d -maxdepth 1| grep exp\_$1 `
do
  for r in run_1 run_2 run_3 run_4 run_5
  do
    c=$(basename $d)
    a=$(./analyze.py $r/$c)
    m=$(./migrate-count.py $r/$c)
    o=$(./check-order.py $r/$c)
    t=$(./avg_mig_time.py $r/$c/)
    echo $a $m $o $t
  done
done