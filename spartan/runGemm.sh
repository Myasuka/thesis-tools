#!/bin/bash
echo ">>>>>>>   start running Spartan with 40 workers   <<<<<<<"

echo "large K case matrix multiplication"
general=(5000000 10000000 20000000 40000000)
for d in ${general[@]}
do
  time ./gemm.py 500 d 500
  sleep 10
done
sleep 60

echo "general case matrix multiplication"
general=(25000 30000)
for d in ${general[@]}
do
  time ./gemm.py d d d
  sleep 10
done
sleep 60

echo "large M and N case matrix multiplication"
general=(100000 200000 300000 400000)
for d in ${general[@]}
do
  time ./gemm.py d 500 d
  sleep 10
done
sleep 60

echo "large with small case multiplication"
general=(500000 1000000 5000000)
for d in ${general[@]}
do
  time ./gemm.py d 1000 1000
  sleep 10
  time ./gemm.py d 10000 1000
  sleep 10
done
