package org.apache.spark.mllib

import java.util.Calendar

import breeze.linalg.{DenseVector => BDV}
import org.apache.spark.mllib.linalg.distributed.{BlockMatrix, IndexedRow, IndexedRowMatrix}
import org.apache.spark.mllib.linalg.{Matrix, Vectors}
import org.apache.spark.{SparkConf, SparkContext}

object MllibMatrix {
  def main(args: Array[String]) {
    if (args.length < 5) {
      println("usage: org.apache.spark.mllib.MllibMatrix <matA rows> <matA cols> <matB cols> " +
        "<sub-block row> <sub-block col> <parallelism>")
      System.exit(1)
    }
    val rowsA = args(0).toInt
    val colsA, rowsB = args(1).toInt
    val colsB = args(2).toInt
    val subRow = args(3).toInt
    val subCol = args(4).toInt
    val partitionNumA = rowsA / subRow
    val partitionNumB = rowsA / subRow
    val parallelism = if (args.length > 5) { args(5).toInt } else 320
    val conf = new SparkConf()
    conf.registerKryoClasses(Array(classOf[IndexedRow], classOf[BlockMatrix], classOf[Matrix]))
    val sc = new SparkContext(conf)

    val indexedRowsA = sc.parallelize(0 until rowsA, parallelism)
      .map(row =>
      IndexedRow(row.toLong, Vectors.dense(BDV.rand[Double](colsA).toArray)))
    val matA = new IndexedRowMatrix(indexedRowsA, rowsA, colsA)
    val indexedRowsB = sc.parallelize(0 until rowsB, parallelism)
      .map(row =>
      IndexedRow(row.toLong, Vectors.dense(BDV.rand[Double](colsB).toArray)))
    val matB = new IndexedRowMatrix(indexedRowsB, rowsB, colsB)
    println(s"mllib-matrix multiplication arguments: ${args.mkString(" ")}," +
      s" ${Calendar.getInstance().getTime}")
    val t0 = System.currentTimeMillis()
    val blkMatA = matA.toBlockMatrix(subRow, subCol)
    val blkMatB = matB.toBlockMatrix(subRow, subCol)
    val result = blkMatA.multiply(blkMatB)
    result.blocks.count()
    println(s"multiplication use time: ${System.currentTimeMillis() - t0} millis, " +
      s"${Calendar.getInstance().getTime}")
    sc.stop()
  }

}
