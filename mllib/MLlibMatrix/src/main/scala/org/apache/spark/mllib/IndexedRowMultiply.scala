package org.apache.spark.mllib

import java.util.{Calendar, Random}

import breeze.linalg.{DenseVector => BDV}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.linalg.{Matrices, Vectors, Matrix}
import org.apache.spark.mllib.linalg.distributed.{IndexedRowMatrix, BlockMatrix, IndexedRow}


object IndexedRowMultiply {
  def main(args: Array[String]) {
    if (args.length < 4) {
      println("usage: IndexedRowMultiply <matA rows> <matA cols> <matB cols> " +
        "<partitions>")
      System.exit(1)
    }
    val rowsA = args(0).toInt
    val colsA, rowsB = args(1).toInt
    val colsB = args(2).toInt
    val parallesim = args(3).toInt
    val conf = new SparkConf()
    conf.registerKryoClasses(Array(classOf[IndexedRow], classOf[BlockMatrix], classOf[Matrix]))
    val sc = new SparkContext(conf)

    val indexedRowsA = sc.parallelize(0 until rowsA, parallesim)
      .map(row =>
      IndexedRow(row.toLong, Vectors.dense(BDV.rand[Double](colsA).toArray)))
    val matA = new IndexedRowMatrix(indexedRowsA, rowsA, colsA)
    val rnd = new Random()
    val matB = Matrices.rand(rowsB, colsB, rnd)
    val t0 = System.currentTimeMillis()
    val result = matA.multiply(matB)
    println(s"mllib-matrix IndexedRowMatrix multiply matrix arguments: ${args.mkString(" ")}," +
      s" ${Calendar.getInstance().getTime}")
    result.rows.count()
    println(s"multiplication use time: ${System.currentTimeMillis() - t0} millis, " +
      s"${Calendar.getInstance().getTime}")
    sc.stop()
  }
}
