import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import $file.common

object v {
  val scala = "2.13.10"
  val spire = ivy"org.typelevel::spire:0.18.0"
  val evilplot = ivy"io.github.cibotech::evilplot:0.9.0"
  val oslib =  ivy"com.lihaoyi::os-lib:0.9.1"
  val chiselCrossVersions = Map(
    "5.0.0" -> (ivy"org.chipsalliance::chisel:5.0.0", ivy"org.chipsalliance:::chisel-plugin:5.0.0"),
  )

  val scalatest = ivy"org.scalatest::scalatest:3.2.0"
  val scalapar = ivy"org.scala-lang.modules::scala-parallel-collections:1.0.4"
}

object arithmetic extends Cross[Arithmetic](v.chiselCrossVersions.keys.toSeq)

object arithmetictest extends Cross[ArithmeticTest](v.chiselCrossVersions.keys.toSeq)

trait Arithmetic
  extends common.ArithmeticModule
    with ScalafmtModule
    with Cross.Module[String] {

  override def scalaVersion = T(v.scala)

  override def millSourcePath = os.pwd / "arithmetic"

  def spireIvy = v.spire

  def evilplotIvy = v.evilplot

  def oslibIvy = v.oslib

  def chiselModule = None

  def chiselPluginJar = None

  def chiselIvy = Some(v.chiselCrossVersions(crossValue)._1)

  def chiselPluginIvy = Some(v.chiselCrossVersions(crossValue)._2)
}

trait ArithmeticTest
  extends common.ArithmeticTestModule
    with Cross.Module[String] {

  override def scalaVersion = T(v.scala)

  override def millSourcePath = os.pwd / "tests"

  def arithmeticModule = arithmetic(crossValue)

  def scalatestIvy = v.scalatest

  def scalaparIvy = v.scalapar

  def spireIvy = v.spire

  def evilplotIvy = v.evilplot

  def oslibIvy = v.oslib

  def chiselModule = None

  def chiselPluginJar = None

  def chiselIvy = Some(v.chiselCrossVersions(crossValue)._1)

  def chiselPluginIvy = Some(v.chiselCrossVersions(crossValue)._2)
}
