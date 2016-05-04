#!/usr/bin/env bash
JAR=/home/hadoop/tangyun/thesis/marlin-0.4-SNAPSHOT.jar
SUBMIT=/home/hadoop/tangyun/thesis/spark-1.5.3-SNAPSHOT-bin-hadoop2.6/bin/spark-submit
PARAM=' --class  cn.edu.nju.pasa.octopus.spark.examples.SparseMultiply   '

# usage: usage: SparseMultiply <matrixA row length> <matrixA column length> <matrixB column length> <density> <mode>
#"mode 1: SparseVecMatrix multiply SparseVecMatrix with CRM algorithm")
#"mode 2: SparseVecMatrix multiply SparseVecMatrix with sparse to dense vector")
#"mode 3: BlockMatrix multiply BlockMatrix with sparse format")
#"mode 4: BlockMatrix multiply BlockMatrix with dense format")
#"mode 5: Dense BlockMatrix multiply Sparse BlockMatrix with dense-sparse format")
#"mode 6: Dense BlockMatrix multiply Sparse BlockMatrix with dense-dense format")


density=(0.005 0.008 0.01 0.02 0.04 0.05 0.06 0.08 0.1 0.15 0.2 0.25 0.3 0.35 0.4 0.5)
for i in $(seq 1 1 3)
do
    echo "itertion: spmm original sparse-sparse marlin density compare *****">>spmm-marlin-density-4-27th.stdout
    echo "********************************">>spmm-marlin-density-4-27th.stdout
    for d in ${density[@]}
    do
       nohup $SUBMIT $PARAM --name c-spmm-30k30k30k-$d-858 $JAR 30000 30000 30000 $d 3 8 5 8 2>>stderr 1>>spmm-marlin-density-4-27th.stdout
       sleep 20
       nohup $SUBMIT $PARAM --name c-spmm-30k30k30k-$d-949 $JAR 30000 30000 30000 $d 3 9 4 9 2>>stderr 1>>spmm-marlin-density-4-27th.stdout
       sleep 20
    done
done

echo ">>>>>>>>>>>>>>>> > > > > > > >>>>>>>>>>>>>>>>>>>>> > > > > > >>>>>>>>>>>>>>>>>>>> > > > > > > >>>>>>>>>>>>>>>>>>>>>>>>>>>"

density=(0.005 0.008 0.01 0.02 0.04 0.05 0.06 0.08 0.1 0.15 0.2 0.25 0.3 0.35 0.4 0.5)
for i in $(seq 1 1 2)
do
    echo "itertion: spmm to dense-dense marlin density compare *****">>spmm-marlin-density-4-27th.stdout
    echo "********************************">>spmm-marlin-density-4-27th.stdout
    for d in ${density[@]}
    do
       nohup $SUBMIT $PARAM --name gemm-30k30k30k-$d-858 $JAR 30000 30000 30000 $d 4 8 5 8 2>>stderr 1>>spmm-marlin-density-4-27th.stdout
       sleep 20
       nohup $SUBMIT $PARAM --name gemm-30k30k30k-$d-949 $JAR 30000 30000 30000 $d 4 9 4 9 2>>stderr 1>>spmm-marlin-density-4-27th.stdout
       sleep 20
    done
done

