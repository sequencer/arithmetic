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

  dut.io.input.bits.a             := verificationModule.toDUT.bits.a
  dut.io.input.bits.b             := verificationModule.toDUT.bits.b
  dut.io.input.bits.op            := verificationModule.toDUT.bits.op
  dut.io.input.bits.roundingMode  := verificationModule.toDUT.bits.roundingMode
  dut.io.input.bits.refOut        := verificationModule.toDUT.bits.refOut
  dut.io.input.bits.refFlags      := verificationModule.toDUT.bits.refFlags
  dut.io.input.valid              := verificationModule.toDUT.valid
  verificationModule.toDUT.ready  := dut.io.input.ready

}


