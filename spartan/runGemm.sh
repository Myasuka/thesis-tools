#!/bin/bash
echo ">>>>>>>   start running Spartan with 80 workers   <<<<<<<"

echo "large K case matrix multiplication"
largek=(5000000 10000000 20000000 40000000)
for d in ${largek[@]}
do
  time ./gemm.py 500 $d 500
  sleep 10
done
sleep 60

echo "general case matrix multiplication"
general=(25000 30000)
for d in ${general[@]}
do
  time ./gemm.py $d $d $d
  sleep 30
done
sleep 60

echo "large M and N case matrix multiplication"
largemn=(100000 200000 300000 400000)
for d in ${largemn[@]}
do
  time ./gemm.py $d 500 $d
  sleep 30
done
sleep 60

echo "large with small case multiplication"
largesmall=(500000 1000000 5000000)
for d in ${largesmall[@]}
do
  time ./gemm.py $d 1000 1000
  sleep 30
  time ./gemm.py $d 10000 1000
  sleep 30
done
