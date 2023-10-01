package tests

import chisel3._
import chisel3.util._
import float._

/** num for input number
  *
  * input.valid need cancel
  *
  * in
  *
  * */
class DUT(expWidth:Int, sigWidth:Int) extends Module {

  val io = IO(new Bundle {
    val input = Flipped(Decoupled(new DUTInput(expWidth, sigWidth)))
    val expected = Input(new Reference(expWidth, sigWidth))

    val actual = new Bundle {
      val out = Output(Bits((expWidth + sigWidth).W))
      val exceptionFlags = Output(Bits(5.W))
    }

    val check = Output(Bool())
    val pass = Output(Bool())
  })

  val ds = Module(new DivSqrt(expWidth: Int, sigWidth: Int))
  ds.input.valid := io.input.valid
  ds.input.bits.sqrt := io.input.valid
  ds.input.bits.a := io.input.bits.a
  ds.input.bits.b := io.input.bits.b
  ds.input.bits.roundingMode := io.input.bits.roundingMode
  /** @todo */
  io.input.ready := ds.input.ready

  // collect result
  io.actual.out := ds.output.bits.result
  io.actual.exceptionFlags := ds.output.bits.exceptionFlags


  val resultError = io.actual.out =/= io.expected.out
  val flagError = io.actual.exceptionFlags =/= io.expected.exceptionFlags

  io.check := ds.output.valid
  io.pass := !(ds.output.valid && (resultError || flagError))

}

class DUTInput(expWidth: Int, sigWidth: Int) extends Bundle {
  val a = Bits((expWidth + sigWidth).W)
  val b = Bits((expWidth + sigWidth).W)
  val op = UInt(2.W)
  val roundingMode = UInt(3.W)
}

class Reference(expWidth: Int, sigWidth: Int) extends Bundle {
  val out = UInt((expWidth + sigWidth).W)
  val exceptionFlags = UInt(5.W)
}




