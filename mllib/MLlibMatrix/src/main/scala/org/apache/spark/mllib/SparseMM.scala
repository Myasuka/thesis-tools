package org.apache.spark.mllib

import java.util.Calendar

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.{Matrices, Matrix}
import org.apache.spark.mllib.linalg.distributed.{BlockMatrix, IndexedRow}


object SparseMM {
  def main(args: Array[String]) {
    if (args.length < 8) {
      println("usage: org.apache.spark.mllib.SparseMM <matA rows> <matA cols> <matB cols> " +
        "<sub-blockA row> <sub-blockA col> <sub-blockB col> <density> <mode> <parallelism>")
      println("mode 1 means sparse-sparse matrix multiplication with given density")
      System.exit(1)
    }
    val rowsA = args(0).toInt
    val colsA, rowsB = args(1).toInt
    val colsB = args(2).toInt
    val subRowA = args(3).toInt
    val subColA, subRowB = args(4).toInt
    val subColB = args(5).toInt
    val density = args(6).toDouble
    val blksRowA = rowsA / subRowA
    val blksColA = colsA / subColA

    val blksRowB = rowsB / subRowB
    val blksColB = colsB / subColB

    val parallelism = if (args.length > 8) { args(8).toInt } else 320
    val conf = new SparkConf()
    conf.registerKryoClasses(Array(classOf[IndexedRow], classOf[BlockMatrix], classOf[Matrix]))
    val sc = new SparkContext(conf)
    val blkIdsA = sc.parallelize(for( i<- (0 until blksRowA); j<- (0 until blksColA)) yield (i, j))
    val blksA = blkIdsA.map(id => (id, Matrices.sprand(math.min(subRowA, rowsA - id._1 * subRowA),
      math.min(subColA, colsA - id._1 * subColA), density, new java.util.Random())))
    val matA = new BlockMatrix(blksA, subRowA, subColA)

    val blkIdsB = sc.parallelize(for( i<- (0 until blksRowB); j<- (0 until blksColB)) yield (i, j))
    val blksB = blkIdsB.map(id => (id, Matrices.sprand(math.min(subRowB, rowsB - id._1 * subRowB),
      math.min(subColB, colsB - id._1 * subColB), density, new java.util.Random())))
    val matB = new BlockMatrix(blksB, subRowB, subColB)
    println(s"mllib sparse-sparse matrix multiplication arguments: ${args.mkString(" ")}," +
      s" ${Calendar.getInstance().getTime}")

    val start = System.currentTimeMillis()
    val result = matA.multiply(matB)
    result.blocks.count()
    val end = System.currentTimeMillis()
    println(s"sparse-sparse matrix multiplication used time: ${end - start} mills")
    sc.stop()


  }
}
