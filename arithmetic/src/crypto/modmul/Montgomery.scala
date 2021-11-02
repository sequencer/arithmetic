package crypto.modmul

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.experimental.decode.TruthTable
import chisel3.util.{Counter, Mux1H}

class Montgomery(pWidth: Int = 4096, addCycles: Int = 2, val addPipe: Int) extends Module {
  val p = IO(Input(UInt(pWidth.W)))
  val pPrime = IO(Input(Bool()))
  val a = IO(Input(UInt(pWidth.W)))
  val b = IO(Input(UInt(pWidth.W)))
  val bp = IO(Input(UInt((pWidth + 1).W))) // b + p
  val valid = IO(Input(Bool())) // input valid
  val out = IO(Output(UInt(pWidth.W)))

  val t = Reg(UInt((pWidth + 2).W))
  val u = Reg(Bool())
  val i = Reg(UInt(pWidth.W))
  val nextT = Reg(UInt((pWidth + 2).W))

  // 4096 multicycle prefixadder
  val adder = Module(new DummyAdd(pWidth, addPipe))
  val addCounter = Counter(addCycles)

  // Control Path
  object StateType extends ChiselEnum {
    val s0 = Value("b00001".U) // t = 0
    val s1 = Value("b00010".U) // loop
    val s2 = Value("b00100".U) // loop add done
    val s3 = Value("b01000".U) // if-then
    val s4 = Value("b10000".U) // done
  }

  val state = RegInit(StateType.s0)
  val isAdd = (state.asUInt() & "b01010".U).orR()
  adder.valid := isAdd
  val addDone = if (addPipe != 0) Counter(isAdd, addPipe)._2 else true.B
  state := chisel3.util.experimental.decode
    .decoder(
      state.asUInt() ## addDone ## valid ## i.head(1), {
        val Y = "1"
        val N = "0"
        val DC = "?"
        def to(
          stateI:  String,
          addDone: String = DC,
          valid:   String = DC,
          iHead:   String = DC
        )(stateO:  String
        ) = s"$stateI$addDone$valid$iHead->$stateO"
        val s0 = "00001"
        val s1 = "00010"
        val s2 = "00100"
        val s3 = "01000"
        val s4 = "10000"
        TruthTable(
          Seq(
            to(s0, valid = N)(s0),
            to(s0, valid = Y)(s1),
            to(s1, addDone = Y)(s2),
            to(s1, addDone = N)(s1),
            to(s2, iHead = Y)(s1),
            to(s2, iHead = N)(s3),
            to(s3, addDone = Y)(s4),
            to(s3, addDone = N)(s3),
            to(s4, valid = Y)(s1),
            to(s4, valid = N)(s4),
            /*
              s0 -> s0,  input invalid
              s0 -> s1,  input valid
              s1 -> s2,  add done
              s1 -> s1,  add not done
              s2 -> s1,  msb[i] != 1
              s2 -> s3,  msb[i] == 1
              s3 -> s4,  add done
              s3 -> s3,  add not done
              s4 -> s1, input valid
              s4 -> s4, input invalid
             */
            "???????"
          ).mkString("\n")
        )
      }
    )
    .asTypeOf(StateType.Type())

  i := Mux1H(
    Map(
      state.asUInt()(1) -> 1.U,
      state.asUInt()(2) -> i.rotateLeft(1)
    )
  )

  adder.a := t
  adder.b := Mux1H(
    Map(
      (state.asUInt()(1) && ((a & i).orR) && u) -> bp,
      (state.asUInt()(1) && ((a & i).orR)) -> b,
      (state.asUInt()(1) && u) -> p,
      // last select
      state.asUInt()(3) -> -p
    )
  )

  nextT := Mux1H(
    Map(
      (state.asUInt()(2) || state.asUInt()(3) || (state.asUInt()(4) && (adder.z.head(1).asBool))) -> adder.z,
      (state.asUInt()(4) && (!adder.z.head(1).asBool)) -> t,
      state.asUInt()(0) -> 0.U
    )
  ) >> 1

  t := nextT

  u := Mux1H(
    Map(
      state.asUInt()(0) -> (a(0).asUInt & b(0).asUInt & pPrime.asUInt),
      state.asUInt()(3) -> ((nextT(0) + (a & i).orR & b(0)) & pPrime.asUInt)
    )
  )
  out := t
}
