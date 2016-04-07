#!/bin/bash
# generate random sparse matrix text csv file

sparsity=(0.005 0.01 0.02 0.04 0.06 0.08 0.1 0.15 0.2 0.25 0.3 0.35 0.4)
dimension=(25000 30000)
for d in ${dimension[@]}
do
  for s in ${sparsity[@]}
  do
    echo "generating sci sparse matrix dimension: $d x $d with density: $s"
    time scala -DXmx8g -cp scidb/RandomMatrix/target/scala-2.10/randommatrix_2.10-1.0.jar RandomSpMat $d $d $s ../sci-mats/sci-$d-$d-$s
    sleep 8
  done
done
