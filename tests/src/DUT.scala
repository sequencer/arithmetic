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
    val input = IO(Flipped(Decoupled(new DutInterface(expWidth, sigWidth))))

    val actual = IO(new Bundle {
      val out = Output(Bits((expWidth + sigWidth).W))
      val exceptionFlags = Output(Bits(5.W))
    })

    val check = IO(Output(Bool()))
    val pass = IO(Output(Bool()))


  val ds = Module(new DivSqrt(expWidth: Int, sigWidth: Int))
  ds.input.valid :=  input.valid
  ds.input.bits.sqrt :=  input.valid
  ds.input.bits.a :=  input.bits.a
  ds.input.bits.b :=  input.bits.b
  ds.input.bits.roundingMode :=  input.bits.roundingMode
  /** @todo */
   input.ready := ds.input.ready

  // collect result
   actual.out := ds.output.bits.result
   actual.exceptionFlags := ds.output.bits.exceptionFlags


  val resultError =  actual.out =/=  input.bits.refOut
  val flagError =  actual.exceptionFlags =/=  input.bits.refFlags

   check := ds.output.valid
   pass := !(ds.output.valid && (resultError || flagError))

}

class DutInterface(expWidth: Int, sigWidth: Int) extends Bundle {
  val a = UInt((expWidth + sigWidth).W)
  val b = UInt((expWidth + sigWidth).W)
  val op = UInt(2.W)
  val roundingMode = UInt(3.W)
  val refOut = UInt((expWidth + sigWidth).W)
  val refFlags = UInt(5.W)
}




