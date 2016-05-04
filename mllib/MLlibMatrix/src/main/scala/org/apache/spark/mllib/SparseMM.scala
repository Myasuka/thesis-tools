package org.apache.spark.mllib

import java.util.Calendar

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.{Matrices, Matrix}
import org.apache.spark.mllib.linalg.distributed.{BlockMatrix, GridPartitioner, IndexedRow}


object SparseMM {
  def main(args: Array[String]) {
    if (args.length < 7) {
      println("usage: org.apache.spark.mllib.SparseMM <matA rows> <matA cols> <matB cols> " +
        "<num of sub-blockA by row> <num of sub-blockA by col> <num of sub-blockB by col> <mode> <density> <parallelism>")
      println("mode sp means sparse-sparse matrix multiplication with given density")
      println("mode de means dense-sparse matrix multiplication with given density")
      println("example: org.apache.spark.mllib.SparseMM 30000 30000 30000 8 5 8 sp 0.01 320")
      System.exit(1)
    }
    val rowsA = args(0).toInt
    val colsA, rowsB = args(1).toInt
    val colsB = args(2).toInt
    val blksRowA = args(3).toInt
    val blksColA, blksRowB = args(4).toInt
    val blksColB = args(5).toInt
    val mode = args(6)
    val density = args(7).toDouble

//    require(rowsA % blksRowA == 0 & colsA % blksColA == 0 & colsB % blksColB == 0,
//      "to avoid the bottom edge or the right edge, <num of sub-blockA by row> <num of sub-blockA by col>" +
//        " <num of sub-blockB by col> should all be divisible by rows or cols")
    val subRowA = math.ceil(rowsA * 1.0 / blksRowA).toInt
    val subColA, subRowB = math.ceil(colsA * 1.0 / blksColA).toInt
    val subColB = math.ceil(colsB * 1.0 / blksColB).toInt

    val parallelism = if (args.length > 8) {
      args(8).toInt
    } else 320

    val conf = new SparkConf()
    conf.registerKryoClasses(Array(classOf[IndexedRow], classOf[BlockMatrix], classOf[Matrix]))
    val sc = new SparkContext(conf)
    val blkIdsA = sc.parallelize(for (i <- (0 until blksRowA); j <- (0 until blksColA)) yield (i, j), blksRowA * blksColA)
    val matA = if (mode == "sp") {
      // sparse matrix
      println(s"mllib sparse-sparse matrix multiplication arguments: ${args.mkString(" ")}," +
        s" ${Calendar.getInstance().getTime}")
      val blksA = blkIdsA.map(id => (id, Matrices.sprand(math.min(subRowA, rowsA - id._1 * subRowA),
        math.min(subColA, colsA - id._2 * subColA), density, new java.util.Random())))
      new BlockMatrix(blksA, subRowA, subColA)
    } else {
      // dense matrix
      println(s"mllib dense-sparse matrix multiplication arguments: ${args.mkString(" ")}," +
        s" ${Calendar.getInstance().getTime}")
      val blksA = blkIdsA.map(id => (id, Matrices.rand(math.min(subRowA, rowsA - id._1 * subRowA),
        math.min(subColA, colsA - id._2 * subColA), new java.util.Random())))
      new BlockMatrix(blksA, subRowA, subColA)
    }

    //    println(s"matA rowsPerBlock: ${matA.rowsPerBlock}, matA colsPerBlock: ${matA.colsPerBlock}")
    //    println(s"matA layout: ${matA.numRowBlocks}, ${matA.numColBlocks}")

    val blkIdsB = sc.parallelize(for (i <- (0 until blksRowB); j <- (0 until blksColB)) yield (i, j), blksRowB * blksColB)
    val blksB = blkIdsB.map(id => (id, Matrices.sprand(math.min(subRowB, rowsB - id._1 * subRowB),
      math.min(subColB, colsB - id._2 * subColB), density, new java.util.Random())))
    val matB = new BlockMatrix(blksB, subRowB, subColB)

    //    println(s"matB rowsPerBlock: ${matB.rowsPerBlock}, matB colsPerBlock: ${matB.colsPerBlock}")
    //    println(s"matB layout: ${matB.numRowBlocks}, ${matB.numColBlocks}")

    val start = System.currentTimeMillis()
    val result = matA.multiply(matB)
    result.blocks.count()
    val end = System.currentTimeMillis()
    val resultPartitioner = GridPartitioner(matA.numRowBlocks, matB.numColBlocks,
      math.max(matA.blocks.partitions.length, matB.blocks.partitions.length))
    println(s"parallelism in sub-block mm: ${resultPartitioner.numPartitions}")
    if (mode == "sp") {
      println(s"mllib sparse-sparse matrix multiplication used time: ${(end - start) / 1000.0} seconds")
    } else {
      println(s"mllib dense-sparse matrix multiplication used time: ${(end - start) / 1000.0} seconds")
    }
    sc.stop()


  }
}
