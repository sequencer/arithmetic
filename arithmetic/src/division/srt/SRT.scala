package division.srt

import breeze.linalg._
import breeze.plot._

/** Base SRT class.
  *
  * @param radix is the radix of SRT.
  *              @note 5.2
  * @param quotientSet is the min and max of quotient-digit set
  *                    @note 5.6
  * @param ulpN ulp is the unit in the last position, defined by `pow(r, -uplN)`
  *             @note 5.2
  * @param normD normalized divider range from `pow(2, _._1)` to `pow(2, _._2)`
  * @param normX normalized dividend range from `pow(2, _._1)` to `pow(2, _._2)`
  */
case class SRT(
  radix:       Int,
  quotientSet: (Int, Int),
  ulpN:        Int = 0,
  normD:       (Int, Int) = (-1, 0),
  normX:       (Int, Int) = (-1, 0)) {
  val a: Int = max(math.abs(quotientSet._1), math.abs(quotientSet._2))
  require(quotientSet._1 < 0, quotientSet._2 > 0)
  // @note 5.7
  require(a >= (radix + 1) / 2)
  // @note 5.3
  require(normD._1 < normD._2)
  require(normX._1 < normX._2)
  val xMin: Double = math.pow(2, normX._1)
  val xMax: Double = math.pow(2, normX._2)
  val dMin: Double = math.pow(2, normD._1)
  val dMax: Double = math.pow(2, normD._2)

  /** redundancy factor
    * @note 5.8
    */
  val rou: Double = a.toDouble / (radix - 1)

  override def toString: String = s"SRT$radix with quotient set: ${quotientSet._1 to quotientSet._2}"
  // @note 5.8s
  assert((rou > 1.0 / 2) && (rou <= 1))

  /** P-D Diagram
    * @note Graph 5.17(b)
    */
  def pdDiagram(): Unit = {
    val fig: Figure = Figure()
    val p:   Plot = fig.subplot(0)
    val x:   DenseVector[Double] = linspace(dMin, dMax)

    val (uk, lk) = (quotientSet._1 to quotientSet._2).map { k: Int =>
      (plot(x, x * uRate(k), name = s"U($k)"), plot(x, x * lRate(k), name = s"L($k)"))
    }.unzip

    p ++= uk ++= lk

    p.xlabel = "d"
    p.ylabel = "rω[j]"
    val scale = 1.1
    p.xlim(0, xMax * scale)
    p.ylim((quotientSet._1 - rou) * xMax * scale, (quotientSet._2 + rou) * xMax * scale)
    p.title = s"P-D Graph of $this"
    p.legend = true
    fig.saveas("pd.pdf")
  }

  /** slope factor of U_k
    * @note 5.56
    */
  def uRate(k: Int): Double = k + rou

  /** slope factor of L_k
    * @note 5.56
    */
  def lRate(k: Int): Double = k - rou

  /** Robertson Diagram
    * @note Graph 5.17(a)
    */
  def robertsonDiagram(d: Double): Unit = {
    require(d > dMin && d < dMax)
    val fig: Figure = Figure()
    val p:   Plot = fig.subplot(0)

    p ++= (quotientSet._1 to quotientSet._2).map { k: Int =>
      val xrange: DenseVector[Double] = linspace((k - rou) * d, (k + rou) * d)
      plot(xrange, xrange - k * d, name = s"$k")
    }

    p.xlabel = "rω[j]"
    p.ylabel = "ω[j+1]"
    p.xlim(-radix * rou * dMax, radix * rou * dMax)
    p.ylim(-rou * d, rou * d)
    p.title = s"Robertson Graph of $this divisor: $d"
    p.legend = true
    fig.saveas("robertson.pdf")
  }
}
