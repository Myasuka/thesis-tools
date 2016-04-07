package org.apache.spark.mllib

import java.util.Random

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.DenseMatrix

object Test {
  def main(args: Array[String]) {
    val rnd = new Random()
    val m = DenseMatrix.rand(10, 10, rnd)
    println(s"m(0, 0): ${m(0, 0)}")
    m(0, 0) -= 10
    println(s"changed, m(0, 0): ${m(0, 0)}")


    val sc = new SparkContext(new SparkConf())
    import org.apache.spark.HashPartitioner
    val partitioner = new HashPartitioner(20)
    val X = sc.parallelize(1 to 120, 20).map(t => (t, 2*t)).partitionBy(partitioner).cache
    val Y = sc.parallelize(1 to 120, 20).map(t => (t, 3*t)).partitionBy(partitioner).cache
    val XdotY  = X.join(Y, partitioner).mapValues(v =>  v._1 * v._2)
    val XpulsY = X.join(Y, partitioner).mapValues(v => v._1 + v._2)
    val result = XdotY.join(XpulsY).mapValues(v => v._1 - v._2)
  }
}
