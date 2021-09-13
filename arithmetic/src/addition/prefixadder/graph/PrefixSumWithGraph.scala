package addition.prefixadder.graph

import addition.prefixadder.PrefixSum
import chisel3._

trait HasPrefixSumWithGraphImp extends PrefixSum {
  val prefixGraph: PrefixGraph

  /** Works out the sequence of prefix sum based on the given prefix graph.
   *
   * @param summands the input sequence, also the values which the zero-layer nodes hold.
   * @return the sequence of the final prefix sum.
   */
  override def apply(summands: Seq[(Bool, Bool)]): Vector[(Bool, Bool)] = {
    require(summands.size == prefixGraph.width, "Module width is different to Graph width")

    /** Makes the Hash table that maps all the nodes to their output boolean pairs, i.e. GP pairs.
     *
     * @param level level of the nodes
     * @param x Hash table mapping prefix nodes to their output, i.e., operation results
     *
     * @return the final Hash table containing the complete sequence of prefix sum
     */
    def helper(level: Int, x: Map[PrefixNode, (Bool, Bool)]): Map[PrefixNode, (Bool, Bool)] = if (
      level > prefixGraph.depth
    ) x
    else
      helper(
        level + 1,
        x ++ prefixGraph.level(level).map(node => node -> associativeOp(node.fathers.map(node => x(node)).toSeq)).toMap
      )
    helper(1, prefixGraph.level(0).zip(summands) toMap)
      .filter(dict => prefixGraph.lastLevelNode.contains(dict._1))
      .toSeq
      .sortBy(_._1.bit)
      .map(_._2)
      .toVector
  }
}
