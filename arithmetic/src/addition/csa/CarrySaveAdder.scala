package arithmetic.addition.csa

import chisel3._

/** Abstract class of a Carry Save Adder.
  *
  * A Carry Save Adder is a compressor: before compressing a `m` bits input is required, after compressing, `n` output
  * should encodes the number of 1 from `m`.
  * So `m+1` terms are encoded to `n` bits output.
  *
  * for example a classic csa 3in 2out, which encodes:
  * {{{
  *   input          number of 1     output
  *   000         -> 0            -> 00
  *   100 010 001 -> 1            -> 01
  *   110 101 011 -> 2            -> 10
  *   111         -> 3            -> 11
  * }}}
  *
  * @param inputSize size of input
  * @param outputSize size of output
  * @param compressor a function that takes the index of bit, which returns a [[CSACompressor]]
  * @param width adder width
  */
abstract class CarrySaveAdder(
  val inputSize:  Int,
  val outputSize: Int,
  val compressor: Int => CSACompressor
)(val width:      Int)
    extends MultiIOModule {
  require(math.pow(2, outputSize) >= inputSize, "not enough output bits to encode.")
  override val desiredName: String = this.getClass.getSimpleName + s"_$width"

  val in:  Vec[UInt] = IO(Input(Vec(inputSize, UInt(width.W))))
  val out: Vec[UInt] = IO(Output(Vec(outputSize, UInt(width.W))))

  (out
    .zip(
      Seq
        .tabulate(width)(i => compressor(i))
        .zipWithIndex
        .map {
          case (m, i) => {
            require(m.inputSize == inputSize && m.outputSize == outputSize)
            m.circuit(in.map(_(i)))
          }
        }
        .transpose
    ))
    .foreach { case (o, i) => o := VecInit(i).asUInt }
}
