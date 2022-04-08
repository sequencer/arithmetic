package division.srt

import chisel3._

class OTFInput extends Bundle {
  val quotient = UInt()
  val quotientMinusOne = UInt()
  val selectedQuotientOH = UInt()
}

class OTFOutput extends Bundle {
  val quotient = UInt()
  val quotientMinusOne = UInt()
}

class OTF extends Module {
  val input = IO(Input(new OTFInput))
  val output = IO(Output(new OTFOutput))
  // control

  // datapath

}
