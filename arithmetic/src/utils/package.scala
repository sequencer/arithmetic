import chisel3._

package object utils {

  /** each bits OR together all bits in its right-hand-side.
    *
    * This circuit seems to be high fan-out, but synthesis tool should handle this.
    */
  def leftOr(data: UInt): UInt = VecInit(Seq.tabulate(data.getWidth) { i: Int =>
    VecInit(data.asBools().dropRight(data.getWidth - i - 1)).asUInt().orR()
  }).asUInt()

  /** each bits OR together all bits in its left-hand-side.
    *
    * This circuit seems to be high fan-out, but synthesis tool should handle this.
    */
  def rightOr(data: UInt): UInt = VecInit(Seq.tabulate(data.getWidth) { i: Int =>
    VecInit(data.asBools().drop(i)).asUInt().orR()
  }).asUInt()

  /** find the first one in the lhs. */
  def leftFirstOne(data: UInt): UInt = (~rightOr(data) << 1).asUInt & data

  /** find the first one in the rhs. */
  def rightFirstOne(data: UInt): UInt = (~leftOr(data) << 1).asUInt & data

  def dynamicFindOneFromRightOH(input: Vec[Bool], query: UInt): Vec[Bool] =
    VecInit(
      input
        .scanLeft((0.U, false.B)) {
          case ((prevSum, _), r) =>
            val sum = prevSum + r.asUInt
            (sum, (sum === query) && r)
        }
        .drop(1)
        .map(_._2)
    )
}
