package addition.prefixadder.graph

import scala.math.Ordered.orderingToOrdered

/** This represents the node of a prefix graph.
 * Every node corresponds to certain associative logic operations except those in the zero layer.
 * Buffer nodes are not present.
 * There are several nodes in the same layer/level of a prefix graph. A node has several fan-ins and fan-outs.
 *
 * @param level represents the logic depth of a prefix paragraph.
 * @param fathers contains all the father nodes, i.e. fan-ins, of a child node. They are the nodes directly engaged in the
 *                operation to get this node.
 * @param prefixData records all the zero-layer nodes involved in the operation to get this node.
 * @param index helps sort different nodes in the [[PrefixGraph]], which is constructed as a sequence.
 *
 */
case class PrefixNode(level: Int, fathers: Set[PrefixNode], prefixData: Set[Int], index: Int)
    extends Ordered[PrefixNode] {
  val bit:               Int = prefixData.max
  override def toString: String = s"Node$level-$bit-$index"
  override def compare(that: PrefixNode): Int = (this.bit, this.level).compare(that.bit, that.level)
}

/** Major construction method of [[PrefixNode]] is defined in its companion object.
 */
object PrefixNode {
  var index = 0

  /** Receives a variable numbers of PrefixNodes, and gernerates their child nodes based on certain rules.
   * @param father a variable number of PrefixNode
   * @return PrefixNode
   */
  def apply(father: PrefixNode*): PrefixNode = {
    require(father.map(_.prefixData).reduce(_ intersect _).isEmpty, "father's prefixData has same prefixes")
    require(
      father.flatMap(_.prefixData).toList.sorted.zipWithIndex.map { case (idx, ele) => ele - idx }.toSet.size == 1,
      "prefixData is not continues"
    )

    /** Its 'level' must be one plus the largest level of its father nodes, since buffer nodes are not considered.
     * All its father nodes is sorted to compose the 'fathers'
     * All the 'prefixData' of its father nodes are lumped and sorted to form its own 'prefixData'.
     * */
    new PrefixNode(father.map(_.level).max + 1, father.sorted.toSet, father.flatMap(_.prefixData).toSet, indexInc)
  }

  /**Performs increment of the index from zero.
   * */
  def indexInc = {
    index = index + 1
    index - 1
  }

  /** Produces the zero-layer nodes. Every node in the zero layer represents a unique bit (element)
   * of the input sequence.
   *
   * @param bit indicates the particular bit that the PrefixNode corresponds to.
   * @return a PrefixNode in the zero-layer of a prefix graph
   *
   */
  def apply(bit: Int): PrefixNode = {
    new PrefixNode(0, Set(), Set(bit), indexInc)
  }
}
