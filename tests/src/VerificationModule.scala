package tests

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._


class VerificationModule extends RawModule {

  val clockRate = 2

  val clock = IO(Output(Clock()))
  val reset = IO(Output(Bool()))


  val pokeDUT = IO(Valid(new DUTInput(8,24)))
  val pokeReference  = IO(Output(new Reference(8,24)))

  val ready = IO(Input(Bool()))


  pokeDUT.bits.b := 0.U
  pokeDUT.bits.op := 0.U
  pokeDUT.bits.roundingMode := 0.U
  pokeDUT.valid := true.B

  pokeReference.out := 0.U
  pokeReference.exceptionFlags := 0.U

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
  pokeDUT.bits.a := dpiBasePoke.a

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
  dpiBasePeek.ready := ready


}
