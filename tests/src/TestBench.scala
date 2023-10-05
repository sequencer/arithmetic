package tests

import chisel3._
import float.DivSqrt


class TestBench(expWidth: Int, sigWidth: Int) extends RawModule {
  val clock = Wire(Clock())
  val reset = Wire(Bool())
  val dut = withClockAndReset(clock, reset) {
    Module(
      new DUT(8,24)
    )
  }
  val verificationModule = Module(new VerificationModule)
  clock := verificationModule.clock
  reset := verificationModule.reset

//  dut.input.bits.a             := verificationModule.dutPoke.bits.a
//  dut.input.bits.b             := verificationModule.dutPoke.bits.b
//  dut.input.bits.op            := verificationModule.dutPoke.bits.op
//  dut.input.bits.roundingMode  := verificationModule.dutPoke.bits.roundingMode
//  dut.input.valid              := verificationModule.dutPoke.valid
//  verificationModule.dutPoke.ready  := dut.input.ready

  verificationModule.dutPoke <> dut.input

  verificationModule.dutPeek := dut.output

}


