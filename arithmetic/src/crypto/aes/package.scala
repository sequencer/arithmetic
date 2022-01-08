package crypto

import chisel3._

package object aes {
  /** a 4 * 4 * 8 matrix */
  private[aes] type SignalMatrix = Vec[Vec[UInt]]
  private[aes] type SignalRow = Vec[UInt]
  private[aes] type ConstantMatrix = Seq[Seq[Int]]

  private[aes] def SubBytes(stateMatrix: SignalMatrix, sBoxInOut: (UInt, UInt)): SignalMatrix = ???
  private[aes] def ShiftRows(stateMatrix: SignalMatrix): SignalMatrix = ???
  private[aes] def MixColumns(stateMatrix: SignalMatrix, mulMatrix: ConstantMatrix): SignalMatrix = ???
  private[aes] def AddRoundKey(stateMatrix: SignalMatrix, key: SignalMatrix): SignalMatrix = ???

  private[aes] def g(w3: SignalRow, sBoxInouts: Seq[(UInt, UInt)]): SignalRow = ???
  private[aes] def roundKey(input: SignalRow): SignalRow = ???
}
