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

  dut.io.input.bits.a := verificationModule.pokeDUT.bits.a
  dut.io.input.bits.b := verificationModule.pokeDUT.bits.b
  dut.io.input.bits.op := 0.U
  dut.io.input.bits.roundingMode := verificationModule.pokeDUT.bits.roundingMode
  dut.io.input.valid := verificationModule.pokeDUT.valid

  dut.io.expected.out := verificationModule.pokeReference.out
  dut.io.expected.exceptionFlags := verificationModule.pokeReference.exceptionFlags



}


