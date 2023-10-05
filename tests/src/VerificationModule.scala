package tests

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._


class VerificationModule extends RawModule {

  val clockRate = 2

  val latPeek = 2

  val clock = IO(Output(Clock()))
  val reset = IO(Output(Bool()))


  val toDUT = IO(DecoupledIO(new DutPoke(8,24)))
  val check = IO(Input(Bool()))
  val pass = IO(Input(Bool()))

  val result = IO(Input(UInt(32.W)))
  val fflags = IO(Input(UInt(32.W)))


  val verbatim = Module(new ExtModule with HasExtModuleInline {
    override val desiredName = "Verbatim"
    val clock = IO(Output(Clock()))
    val reset = IO(Output(Bool()))
    setInline(
      "verbatim.sv",
      s"""module Verbatim(
         |  output clock,
         |  output reset
         |);
         |  reg _clock = 1'b0;
         |  always #($clockRate) _clock = ~_clock;
         |  reg _reset = 1'b1;
         |  initial #(${2 * clockRate + 1}) _reset = 0;
         |
         |  assign clock = _clock;
         |  assign reset = _reset;
         |
         |  import "DPI-C" function void dpiInitCosim();
         |  initial dpiInitCosim();
         |
         |  import "DPI-C" function void dpiTimeoutCheck();
         |  always #(${2 * clockRate + 1}) dpiTimeoutCheck();
         |
         |
         |  export "DPI-C" function dpiDumpWave;
         |  function dpiDumpWave(input string file);
         |   $$dumpfile(file);
         |   $$dumpvars(0);
         |  endfunction;
         |
         |  export "DPI-C" function dpiFinish;
         |  function dpiFinish();
         |   $$finish;
         |  endfunction;
         |
         |  export "DPI-C" function dpiError;
         |  function dpiError(input string what);
         |   $$error(what);
         |  endfunction;
         |
         |endmodule
         |""".stripMargin
    )
  })
  clock := verbatim.clock
  reset := verbatim.reset

  val dpiBasePoke = Module(new ExtModule with HasExtModuleInline {
    override val desiredName = "dpiBasePoke"
    val a = IO(Output(UInt(32.W)))
    val clock = IO(Input(Clock()))
    setInline(
      s"$desiredName.sv",
      s"""module $desiredName(
         |  input clock,
         |  output [31:0] a
         |);
         |  import "DPI-C" function void $desiredName(output bit[31:0] a);
         |
         |  always @ (posedge clock) $desiredName(a);
         |endmodule
         |""".stripMargin
    )
  })
  dpiBasePoke.clock := verbatim.clock
//  toDUT.bits.a := dpiBasePoke.a

  val dpiBasePeek = Module(new ExtModule with HasExtModuleInline {
    override val desiredName = "dpiBasePeek"
    val ready = IO(Input(Bool()))
    val clock = IO(Input(Clock()))
    setInline(
      s"$desiredName.sv",
      s"""module $desiredName(
         |  input clock,
         |  input ready
         |);
         |  import "DPI-C" function void $desiredName(input bit ready);
         |
         |  always @ (posedge clock) $desiredName(ready);
         |endmodule
         |""".stripMargin
    )
  })
  dpiBasePeek.clock := verbatim.clock
  dpiBasePeek.ready := toDUT.ready

  val dpiPeekPoke = Module(new ExtModule with HasExtModuleInline {
    override val desiredName = "dpiPeekPoke"
    val clock = IO(Input(Clock()))
    val a = IO(Output(UInt(32.W)))
    val b = IO(Output(UInt(32.W)))
    val op = IO(Output(UInt(2.W)))
    val rm = IO(Output(UInt(3.W)))
    val refOut = IO(Output(UInt(32.W)))
    val refFlags = IO(Output(UInt(5.W)))
    val valid = IO(Output(Bool()))
    setInline(
      s"$desiredName.sv",
      s"""module $desiredName(
         |  output clock,
         |  output valid,
         |  output [31:0] a,
         |  output [31:0] b,
         |  output [1:0] op,
         |  output [2:0] rm,
         |  output [31:0] refOut,
         |  output [4:0]  refFlags
         |);
         |
         |  import "DPI-C" function void $desiredName(
         |  output bit valid,
         |  output bit[31:0] a,
         |  output bit[31:0] b,
         |  output bit[1:0]  op,
         |  output bit[2:0]  rm,
         |  output bit[31:0] refOut,
         |  output bit[4:0]  refFlags
         |  );
         |
         |  always @ (negedge clock) $desiredName(
         |  valid,
         |  a,
         |  b,
         |  op,
         |  rm,
         |  refOut,
         |  refFlags);
         |
         |
         |
         |endmodule
         |""".stripMargin
    )
  })
  dpiPeekPoke.clock       := verbatim.clock
  toDUT.valid             := dpiPeekPoke.valid
  toDUT.bits.a            := dpiPeekPoke.a
  toDUT.bits.b            := dpiPeekPoke.b
  toDUT.bits.op           := dpiPeekPoke.op
  toDUT.bits.roundingMode := dpiPeekPoke.rm
  toDUT.bits.refOut       := dpiPeekPoke.refOut
  toDUT.bits.refFlags     := dpiPeekPoke.refFlags




}
