package division.srt

import breeze.linalg._
import breeze.plot._
import spire.implicits._
import spire.math._

/** Base SRT class.
  *
  * @param radix is the radix of SRT.
  *              @note 5.2
  * @param quotientSet is the min and max of quotient-digit set
  *                    @note 5.6
  * @param ulpN ulp is the unit in the last position, defined by `pow(r, -uplN)`
  *             @note 5.2
  * @param normD normalized divider range
  * @param normX normalized dividend range
  */
case class SRT(
  radix:       Algebraic,
  quotientSet: (Algebraic, Algebraic),
  ulpN:        Algebraic = 0,
  normD:       (Algebraic, Algebraic) = (-1, 0),
  normX:       (Algebraic, Algebraic) = (-1, 0)) {
  val a: Algebraic = (-quotientSet._1).max(quotientSet._2)
  // @note 5.7
  require(a >= radix / 2)
  // @note 5.3
  require(normD._1 < normD._2)
  require(normX._1 < normX._2)
  val xMin: Algebraic = Algebraic(2).pow(normX._1.toInt)
  val xMax: Algebraic = Algebraic(2).pow(normX._2.toInt)
  val dMin: Algebraic = Algebraic(2).pow(normD._1.toInt)
  val dMax: Algebraic = Algebraic(2).pow(normD._2.toInt)

  /** redundancy factor
    * @note 5.8
    */
  val rou: Algebraic = a / (radix - 1)

  override def toString: String = s"SRT$radix with quotient set: from ${-quotientSet._1} to ${quotientSet._2}"
  // @note 5.8s
  assert((rou > 1 / 2) && (rou <= 1))

  /** P-D Diagram
    * @note Graph 5.17(b)
    */
  def pdDiagram(): Unit = {
    val fig: Figure = Figure()
    val p:   Plot = fig.subplot(0)
    val x:   DenseVector[Double] = linspace(dMin.toDouble, dMax.toDouble)

    val (uk, lk) = (quotientSet._1.toBigInt to quotientSet._2.toBigInt).map { k: BigInt =>
      (plot(x, x * uRate(k.toInt).toDouble, name = s"U($k)"), plot(x, x * lRate(k.toInt).toDouble, name = s"L($k)"))
    }.unzip

    p ++= uk ++= lk

    p.xlabel = "d"
    p.ylabel = "rω[j]"
    val scale = 1.1
    p.xlim(0, (xMax * scale).toDouble)
    p.ylim(((quotientSet._1 - rou) * xMax * scale).toDouble, ((quotientSet._2 + rou) * xMax * scale).toDouble)
    p.title = s"P-D Graph of $this"
    p.legend = true
    fig.saveas("pd.pdf")
  }

  /** slope factor of U_k
    * @note 5.56
    */
  def uRate(k: Algebraic): Algebraic = k + rou

  /** slope factor of L_k
    * @note 5.56
    */
  def lRate(k: Algebraic): Algebraic = k - rou

  /** Robertson Diagram
    * @note Graph 5.17(a)
    */
  def robertsonDiagram(d: Algebraic): Unit = {
    require(d > dMin && d < dMax)
    val fig: Figure = Figure()
    val p:   Plot = fig.subplot(0)

    p ++= (quotientSet._1.toInt to quotientSet._2.toInt).map { k: Int =>
      val xrange: DenseVector[Double] = linspace(((Algebraic(k) - rou) * d).toDouble, ((Algebraic(k) + rou) * d).toDouble)
      plot(xrange, xrange - k * d.toDouble, name = s"$k")
    }

    p.xlabel = "rω[j]"
    p.ylabel = "ω[j+1]"
    p.xlim((-radix * rou * dMax).toDouble, (radix * rou * dMax).toDouble)
    p.ylim((-rou * d).toDouble, (rou * d).toDouble)
    p.title = s"Robertson Graph of $this divisor: $d"
    p.legend = true
    fig.saveas("robertson.pdf")
  }
}
