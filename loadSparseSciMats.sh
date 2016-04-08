#!/bin/bash

dimension=(25000 30000)
sparsity=(0.005 0.01 0.02 0.04 0.06 0.08 0.1 0.15 0.2 0.25 0.3 0.35 0.4)

for d in ${dimension[@]}
do
  for s in ${sparsity[@]}
  do
    let rows=d-1
    ss=$(printf "%.0f"  $(expr $s*1000 | bc))
    path=$THESIS/sci-mats/sci-$d-$d-$s
    time /opt/scidb/14.8/bin/loadcsv.py -t NNN -x -a "a${d}_${d}_raw" -s "<row:int64,col:int64,value:double>[i=0:*,1000000,0]" -A "a${d}_s${ss}" -X -S "<value:double>[row=0:$rows,200,0,col=0:$rows,200,0]" -i $path
    sleep 8
  done
done
