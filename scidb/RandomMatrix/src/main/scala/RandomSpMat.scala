import java.io.{BufferedWriter, FileWriter}

import scala.collection.mutable
import scala.util.Random

object RandomSpMat {
  def main(args: Array[String]) {
    if (args.length < 4) {
      println(s"usage RandomSpMat <rows> <cols> <density> <file path>")
      System.exit(1)
    }

    val rows = args(0).toInt
    val cols = args(1).toInt
    val density = args(2).toDouble

    var i = 0

    val colInd = (cols * density).toInt
    val colList = (0 until cols).toList

    val writer = new BufferedWriter(new FileWriter(args(3)))
    println(s"start writing random matrix file with args: ${args.mkString(", ")}")
    while (i < rows) {
      if (i % 10000 == 0) println(s"generating to row line $i")
      val set = new mutable.HashSet[Int]()
      while (set.size < colInd) {
        set.add(Random.nextInt(cols))
      }
      val list = set.toArray
      java.util.Arrays.sort(list)
      for (l <- list) {
        writer.write(s"$i $l ${Random.nextDouble()}\n")
      }
      i += 1
    }
    writer.flush()
    writer.close()
  }
}
