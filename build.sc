import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import mill.scalalib.TestModule.Utest
import coursier.maven.MavenRepository
import $file.dependencies.chisel.build
import $file.common

object v {
  val scala = "2.13.11"
  val spire = ivy"org.typelevel::spire:0.18.0"
  val evilplot = ivy"io.github.cibotech::evilplot:0.9.0"
}

object mychisel extends dependencies.chisel.build.Chisel(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel"
}

object arithmetic extends common.ArithmeticModule with ScalafmtModule { m =>
  def scalaVersion = T(v.scala)
  def chiselModule = Some(mychisel)
  def chiselPluginJar = T(Some(mychisel.pluginModule.jar()))
  def spire: T[Dep] = v.spire
  def evilplot: T[Dep] = v.evilplot
 }
