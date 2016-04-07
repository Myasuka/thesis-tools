#!/bin/bash

dimension=(25000 3000)
sparsity=(0.005 0.01 0.02 0.04 0.06 0.08 0.1 0.15 0.2 0.25 0.3 0.35 0.4)

for d in ${dimension[@]}
do
  for s in ${sparsity[@]}
  do
    let rows=d-1
    /opt/scidb/14.8/bin/loadcsv.py -t NNN -x -a "$d-$d-raw" -s "<row:int64,col:int64,value:double>[i=0:*,1000000,0]" -A "${d}_$s" -X -S "<value:double>[row=0:$rows,250,0,col=0:$rows,250,0]" -i '$THESIS/sci-mats/sci-$d-$d-$s' -v
  done
done
