#!/usr/bin/env bash
JAR=/home/hadoop/tangyun/thesis/marlin-0.4-SNAPSHOT.jar
SUBMIT=/home/hadoop/tangyun/thesis/spark-1.5.3-SNAPSHOT-bin-hadoop2.6/bin/spark-submit
PARAM=' --class  cn.edu.nju.pasa.octopus.spark.examples.SparseMultiply   '

# usage: usage: SparseMultiply <matrixA row length> <matrixA column length> <matrixB column length> <density> <mode>
#("mode 1: SparseVecMatrix multiply SparseVecMatrix with CRM algorithm")
#("mode 2: SparseVecMatrix multiply SparseVecMatrix with sparse to dense vector")
#("mode 3: BlockMatrix multiply BlockMatrix with sparse format")
#("mode 4: BlockMatrix multiply BlockMatrix with dense format")
#"mode 5: Dense BlockMatrix multiply Sparse BlockMatrix with dense-sparse format")
#"mode 6: Dense BlockMatrix multiply Sparse BlockMatrix with dense-dense format")


dim=(20000 30000 40000 50000 60000)
for i in $(seq 1 1 3)
do
    echo "iteration: spmm original sparse-sparse marlin size compare *****">>spmm-marlin-size-4-27th.stdout
    echo "********************************">>spmm-marlin-size-4-27th.stdout
    for d in ${dim[@]}
    do
       nohup $SUBMIT $PARAM --name spmm-dim-$d-858-0.01 $JAR $d $d $d 0.01 3 8 5 8 2>>stderr 1>>spmm-marlin-size-4-27th.stdout
       sleep 20
       nohup $SUBMIT $PARAM --name spmm-dim-$d-858-0.1  $JAR $d $d $d 0.1  3 8 5 8 2>>stderr 1>>spmm-marlin-size-4-27th.stdout
       sleep 20
    done
done

echo ">>> >> > > > > > > > > > > >> > > > > > > > >> > > > >> > > >> > > > >> > > > > > > > > >> > > > > > >> > > > >> > > >> > " >> spmm-marlin-size-4-27th.stdout

dim=(20000 30000 40000 50000 60000)
for i in $(seq 1 1 2)
do
    echo "iteration: spmm to dense-dense marlin size compare *****">>spmm-marlin-size-4-27th.stdout
    echo "********************************">>spmm-marlin-size-4-27th.stdout
    for d in ${dim[@]}
    do
       nohup $SUBMIT $PARAM --name spmm-dim-$d-858-0.01 $JAR $d $d $d 0.01 4 8 5 8 2>>stderr 1>>spmm-marlin-size-4-27th.stdout
       sleep 20
       nohup $SUBMIT $PARAM --name spmm-dim-$d-858-0.1  $JAR $d $d $d 0.1  4 8 5 8 2>>stderr 1>>spmm-marlin-size-4-27th.stdout
       sleep 20
    done
done

