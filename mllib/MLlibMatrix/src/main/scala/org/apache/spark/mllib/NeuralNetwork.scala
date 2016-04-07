package org.apache.spark.mllib

import java.io.File
import java.util.Random

import breeze.linalg.{DenseMatrix => BDM, DenseVector => BDV, csvwrite, min}
import breeze.numerics.sigmoid
import breeze.stats.distributions.{Gaussian, Uniform}
import org.apache.spark.mllib.linalg.distributed.{BlockMatrix, GridPartitioner, IndexedRow, IndexedRowMatrix}
import org.apache.spark.mllib.linalg.{DenseMatrix, Matrices, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, HashSet}

object NeuralNetwork {


  def splitMethod(oldRange: Array[(Int, Int)], newSubBlk: Int):
  Array[ArrayBuffer[(Int, (Int, Int), (Int, Int))]] = {
    val oldBlks = oldRange.length
    val splitStatus = Array.ofDim[ArrayBuffer[(Int, (Int, Int),(Int, Int))]](oldBlks)
    for (i <- 0 until oldBlks){
      val (start, end) = oldRange(i)
      val startId = start / newSubBlk
      val endId = end / newSubBlk
      val num = endId - startId + 1
      //      val tmpBlk = math.ceil((end - start + 1).toDouble / num.toDouble).toInt
      val arrayBuffer = new ArrayBuffer[(Int, (Int, Int), (Int, Int))]()
      var tmp = 0
      for (j <- 0 until num){
        val tmpEnd = min((j + startId + 1) * newSubBlk - 1 - start , end - start)
        arrayBuffer.+=((j + startId, (tmp , tmpEnd), ((tmp + start) % newSubBlk, (tmpEnd + start) % newSubBlk)))
        tmp = tmpEnd + 1
      }
      splitStatus(i) = arrayBuffer
    }
    splitStatus
  }

  def toDisVector(vectors: RDD[(Int, BDV[Int])], splitStatusByRow: Array[ArrayBuffer[(Int, (Int, Int), (Int, Int))]],
                  splitNum: Int, length: Long): RDD[((Int, Int), BDV[Int])] = {
    val mostSplitLen = math.ceil(length.toDouble / splitNum.toDouble).toInt
    //    val splitLen = math.ceil(length.toDouble / mostSplitLen).toInt
    val vecs = vectors.mapPartitionsWithIndex{ (id, iter) =>
      val array = Array.ofDim[(Int, (Int, Int, BDV[Int]))](splitStatusByRow(id).size)
      var count = 0
      val vector = iter.next()._2
      for ((vecId, (oldStart, oldEnd), (newStart, newEnd)) <- splitStatusByRow(id)) {
        array(count) = (vecId, (newStart, newEnd, vector(oldStart to oldEnd)))
        count += 1
      }
      array.toIterator
    }.groupByKey().mapPartitions{ iter =>
      iter.map{ case(vecId, iterable) =>
        val vecLen = if ((vecId + 1) * mostSplitLen > length) {
          (length - vecId * mostSplitLen).toInt
        } else mostSplitLen
        val vector = BDV.zeros[Int](vecLen)
        val iterator = iterable.iterator
        for ((rowStart, rowEnd, vec) <- iterator) {
          vector(rowStart to rowEnd) := vec.asInstanceOf[BDV[Int]]
        }
        ((vecId, 0), vector)
      }
    }
    vecs
  }

  def loadMNISTImages(sc: SparkContext, input: String, vectorLen: Int,
                      blkNum: Int):(BlockMatrix, RDD[((Int, Int), BDV[Int])]) = {
    val t0 = System.currentTimeMillis()
    val values = sc.textFile(input, blkNum)
    values.cache()
    val partitionInfo = values.mapPartitionsWithIndex(
      (id, iter) => Iterator.single[(Int, Int)](id, iter.size)
      , preservesPartitioning = true).collect().toMap
    val partitionIndex = new Array[(Int, Int)](partitionInfo.size)
    var count = 0
    for (i <- 0 until partitionInfo.size) {
      partitionIndex(i) = (count, partitionInfo.get(i).get + count - 1)
      count += partitionInfo.get(i).get
    }
    // generate rows in the format of (index, breeze vector)
    val rows = values.mapPartitionsWithIndex((id, iter) => {
      val (start, end) = partitionIndex(id)
      val indices = start.toLong to end.toLong
      indices.toIterator.zip(iter.map{ line =>
        val items = line.split("\\s+")
        val indicesAndValues = items.tail.map{ item =>
          val indexAndValue = item.split(":")
          // mnist index starts from 1
          val ind = indexAndValue(0).toInt - 1
          val value = indexAndValue(1).toDouble
          (ind, value)
        }
        val array = Array.ofDim[Double](vectorLen)
        for ((i, v) <- indicesAndValues) {
          array.update(i, v)
        }
        Vectors.dense(array)
      }).map{case(id, vector) => IndexedRow(id, vector)} }, preservesPartitioning = true)

    val subVec = math.ceil(count.toDouble / blkNum.toDouble).toInt
    val splitStatusByRow = splitMethod(partitionIndex, subVec)
    val mat = new IndexedRowMatrix(rows, count.toLong, vectorLen)
    val rowsPerBlock = math.ceil(count.toDouble / blkNum.toDouble).toInt
    /** the parameters here mean the sub-block matrix size **/
    println(s"during toBlockMatrix, rowsPerBlock: $rowsPerBlock, colsPerBlock: $vectorLen")
    val blkMat = mat.toBlockMatrix(rowsPerBlock, vectorLen)

    val labels = values.mapPartitionsWithIndex(
      (id, iter) => {
        val array = iter.map{ line =>
          line.split("\\s+")(0).toInt
        }.toArray
        Iterator.single[(Int, BDV[Int])](id, BDV(array))}
      , preservesPartitioning = true)

//    val disVector = new DistributedIntVector(labels)
    val distributedVector = toDisVector(labels, splitStatusByRow, blkNum, count)
      .map{case(vectorId, vec) => (vectorId, vec)}
    values.unpersist()
    println(s"load mnist image used time: ${System.currentTimeMillis() - t0} millis")

    (blkMat, distributedVector)

  }

  def genRandomBlocks(executors: Int, blockEachExecutor: Int,
                      selectedEachExecutor: Int): HashSet[Int] = {
    require(selectedEachExecutor <= blockEachExecutor, s"the sampled blocks " +
      s"on each executor should be less than the total blocks on each executor")
    val uni = new Uniform(0, blockEachExecutor - 1)
    val set = new mutable.HashSet[Int]()
    for (i <- 0 until executors) {
      while (set.size < (i + 1) * selectedEachExecutor) {
        set.+=(uni.sample().toInt + i * blockEachExecutor)
      }
    }
    set
  }

  def dSigmoid(x: Double): Double = {
    (1 - sigmoid(x)) * sigmoid(x)
  }

  def computeDelta(input: BlockMatrix,
                   error: BlockMatrix,
                   rowsPerBlock: Int,
                   colsPerBlock: Int,
                   batchSize: Long ): BlockMatrix = {
    val dActivation = input.blocks.mapPartitions(iter =>
      iter.map{case(blkId, blk) => (blkId, blk.map(x => dSigmoid(x)))}
      , preservesPartitioning = true)

    val result = dActivation.join(error.blocks).mapPartitions(iter =>
      iter.map{ case(blkId, (blk1, blk2)) =>
        val res = Matrices.fromBreeze(blk1.toBreeze :*  blk2.toBreeze)
        (blkId, res)
      }, preservesPartitioning = true)

    new BlockMatrix(result, rowsPerBlock, colsPerBlock, batchSize, colsPerBlock)
  }

  def computeOutputError(output: BlockMatrix, labels: RDD[((Int, Int), BDV[Int])],
                          rowsPerBlock: Int, colsPerBlock: Int,
                          batchSize: Long): BlockMatrix = {
    val result = output.blocks.join(labels).mapValues{case(blk, vec) =>
      for(i <- 0 until vec.length) {
        blk(i, vec(i)) -= 1.0
      }
      blk
    }
    new BlockMatrix(result, rowsPerBlock, colsPerBlock, batchSize, colsPerBlock)
  }

  def computeLayerError(outputDelta: BlockMatrix,
                        weight: DenseMatrix,
                        rowsPerBlock: Int,
                        colsPerBlock: Int,
                        batchSize: Long): BlockMatrix= {
    val result = outputDelta.blocks.mapPartitions(iter =>
      iter.map{case(blkId, blk) =>
        (blkId, Matrices.fromBreeze(blk.multiply(weight).toBreeze))}
      , preservesPartitioning = true)
    new BlockMatrix(result, rowsPerBlock, colsPerBlock, batchSize, colsPerBlock)
  }

  def computeWeightUpd(input: BlockMatrix,
                        delta: BlockMatrix,
                        learningRate: Double,
                        batchSize: Int): BDM[Double] = {
     val result = input.multiply(delta).blocks.collect()
     if( result.size != 1){
       println("the num of weight matrix does not equal to one")
       System.exit(1)
     }
    (result(0)._2.toBreeze * (learningRate / batchSize)).asInstanceOf[BDM[Double]]
  }

  def main(args: Array[String]) {
    if (args.length < 7) {
      println("usage: NeuralNetwork <input path> <iterations>  " +
        "<learningRate> <executors> <block on each executor> " +
        "<selected blocks on each executor> {<layer unit num> ...}")
      System.exit(-1)
    }
    val input = args(0)
    val iterations = args(1).toInt
    val learningRate = args(2).toDouble
    val executors = args(3).toInt
    val blkEachExecutor = args(4).toInt
    val selectedEachExecutor = args(5).toInt
    val layerNum = args(6).toInt
    val conf = new SparkConf()//.setMaster("local[2]").setAppName("test NN local")
    val sc = new SparkContext(conf)
    val vectorLen = 28 * 28
    // when initializing the weight matrix, the elements should be close to zero
    val rnd = new Random()
    var hiddenWeight = DenseMatrix.randn(vectorLen, layerNum, rnd)
    var outputWeight = DenseMatrix.randn(layerNum, 10, rnd)
    println(s"weight matrix updated")
    val totalBlks = executors * blkEachExecutor
    val (oriData, oriLabels) = loadMNISTImages(sc, input, vectorLen, totalBlks)
    val dataSize = oriData.numRows()
    val batchSize = (dataSize * (selectedEachExecutor * 1.0 / blkEachExecutor)).toLong
    println(s"data size of original input data: $dataSize")
    println(s"numBlocks of original input data: ${oriData.numRowBlocks}")
    println(s"count of the label data: ${oriLabels.count()}")
    val rowsPerBlock = math.ceil(dataSize.toDouble / totalBlks.toDouble).toInt
//    val partitioner = GridPartitioner(dataSize.toInt, vectorLen, totalBlks)
    val partitioner = new HashPartitioner(totalBlks)
    val data = new BlockMatrix(oriData.blocks.partitionBy(partitioner), rowsPerBlock ,vectorLen, dataSize, vectorLen)
//    val data = new BlockMatrix(oriData.blocks, rowsPerBlock ,vectorLen, dataSize, vectorLen)
    data.cache()
    println(s"data blocks: ${data.blocks.count()}")
    println(s"original data partitioner: ${data.blocks.partitioner}")
    val labels = oriLabels.partitionBy(partitioner)
    labels.cache()
    println(s"labels partitioner: ${labels.partitioner}")
    println(s"labels count: ${labels.count}")

//    val batchSize: Int = (selectedEachExecutor.toDouble / blkEachExecutor.toDouble * dataSize).toInt

    for (i <- 0 until iterations) {
      val t0 = System.currentTimeMillis()
      val set = genRandomBlocks(executors, blkEachExecutor, selectedEachExecutor)
      println(s"the generate random set size: ${set.size}")
      /** Propagate through the network by mini-batch SGD **/
      val selPartitioner = GridPartitioner(batchSize.toInt, vectorLen, selectedEachExecutor * executors)
      val dd = data.blocks.filter{
        case(blkId, _) => set.contains(blkId._1)}.cache()
      val array = dd.map(t => t._1._1).collect()
      scala.util.Sorting.quickSort(array)
      val map = array.zipWithIndex.toMap
      val inputData = new BlockMatrix(blocks = dd.map {
        case ((i, j), blk) => ((map.get(i).get, j), blk)
      }.partitionBy(selPartitioner)
        ,rowsPerBlock = rowsPerBlock, colsPerBlock = vectorLen, nRows = batchSize , nCols = vectorLen).cache()
      dd.unpersist()
//      inputData.blocks.partitionBy(selPartitioner).cache()
      println(s"inputData partitioner: ${inputData.blocks.partitioner}")
//      val selectedRows = inputData.
      val hiddenLayerInput = new BlockMatrix(inputData.blocks.mapPartitions(
        iter => iter.map{case(blkId, block) =>
          (blkId, block.multiply(hiddenWeight))}
        , preservesPartitioning = true), rowsPerBlock, vectorLen, batchSize, vectorLen).cache()
      println(s"hiddenLayerInput partitioner: ${hiddenLayerInput.blocks.partitioner}")

      val hiddenLayerOut = new BlockMatrix(hiddenLayerInput.blocks.mapValues(_.map(x => sigmoid(x))),
        rowsPerBlock, vectorLen, batchSize , vectorLen).cache()
      println(s"hiddenLayerOut partitioner: ${hiddenLayerOut.blocks.partitioner}")

      val outputLayerInput = new BlockMatrix(hiddenLayerOut.blocks.mapPartitions(
        iter => iter.map{case(blkId, block) =>
          (blkId, block.multiply(outputWeight))}
        , preservesPartitioning = true), rowsPerBlock, vectorLen, batchSize, vectorLen).cache()
      println(s"outputLayerInput partitioner: ${outputLayerInput.blocks.partitioner}")

      val outputLayerOut =  new BlockMatrix(outputLayerInput.blocks.mapValues(_.map(x => sigmoid(x))),
        rowsPerBlock, vectorLen, batchSize , vectorLen).cache()
      println(s"outputLayerOut partitioner: ${outputLayerOut.blocks.partitioner}")


      /** Back Propagate the errors **/
      val selectedLabels = labels.filter{case(blockId, _) => set.contains(blockId._1)}.map{
        case((i,j),v) => ((map.get(i).get, j),v)
      }.partitionBy(selPartitioner).cache()
      val outputError = computeOutputError(outputLayerOut, selectedLabels, rowsPerBlock, vectorLen, batchSize)
      val outputDelta = computeDelta(outputLayerInput, outputError, rowsPerBlock, vectorLen, batchSize)
      println(s"outputError partitioner: ${outputError.blocks.partitioner}")
      println(s"outputDelta partitioner: ${outputDelta.blocks.partitioner}")

      // update the hidden layer
      val hiddenError = computeLayerError(outputDelta, outputWeight.transpose, rowsPerBlock, vectorLen, batchSize)
      val hiddenDelta = computeDelta(hiddenLayerInput, hiddenError, rowsPerBlock, vectorLen, batchSize)
      println(s"hiddenError partitioner: ${hiddenError.blocks.partitioner}")
      println(s"hiddenDelta partitioner: ${hiddenDelta.blocks.partitioner}")

      /** update the weights **/
      val outWeightUpd = computeWeightUpd(hiddenLayerOut.transpose, outputDelta, learningRate, batchSize.toInt)
      outputWeight = Matrices.fromBreeze(outputWeight.toBreeze.asInstanceOf[BDM[Double]] - outWeightUpd).asInstanceOf[DenseMatrix]

      val hiddenWeightUpd = computeWeightUpd(inputData.transpose, hiddenDelta, learningRate, batchSize.toInt)
      hiddenWeight = Matrices.fromBreeze(hiddenWeight.toBreeze.asInstanceOf[BDM[Double]] - hiddenWeightUpd).asInstanceOf[DenseMatrix]

      hiddenLayerInput.blocks.unpersist()
      hiddenLayerOut.blocks.unpersist()
      outputLayerInput.blocks.unpersist()
      inputData.blocks.unpersist()
      outputLayerOut.blocks.unpersist()

      println(s"in iteration $i, used time: ${System.currentTimeMillis() - t0} millis")
    }

    csvwrite(new File("hiddenWeight-mllib"), hiddenWeight.toBreeze)
    csvwrite(new File("outputWeight-mllib"), outputWeight.toBreeze)

    sc.stop()

  }
}
