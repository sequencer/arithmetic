package addition

import addition.csa.common.{CSACompressor2_2, CSACompressor3_2, CSACompressor5_3}
import chisel3._

package object csa {
  def apply[T <: Data](csa: CSACompressor, width: Option[Int] = None)(in: Vec[T]) = {
    val m = new CarrySaveAdder(csa, width.getOrElse(in.flatMap(_.widthOption).max))
    m.in := VecInit(in.map(_.asUInt()))
    m.out
  }

  def c22[T <: Data](in: Vec[T]) = apply(CSACompressor2_2, Some(2))(in)
  def c32[T <: Data](in: Vec[T]) = apply(CSACompressor3_2, Some(3))(in)
  def c53[T <: Data](in: Vec[T]) = apply(CSACompressor5_3, Some(5))(in)
}
