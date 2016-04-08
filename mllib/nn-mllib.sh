#! /usr/bin/env bash
JAR=/home/hadoop/tangyun/final_paper/re-exp/nn/mllibmatrix_2.10-1.0.jar
SUBMIT=/home/hadoop/tangyun/spark-1.4.0-bin-hadoop2.6/bin/spark-submit
PARAM=' --class org.apache.spark.mllib.NeuralNetwork  '

#println("usage: NeuralNetwork <input path> <iterations>  " +
#        "<learningRate> <executors> <block on each executor> " +
#        "<selected blocks on each executor> {<layer unit num> ...}")


nohup $SUBMIT $PARAM --name nn-mallib $JAR hdfs://master:8020/mnist8m.scale 120 0.1 20 100 1 300 2>>nn-mllib-12th.stderr 1>>nn-mllib-12th.stdout
