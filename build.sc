// import Mill dependency
import coursier.maven.MavenRepository
import mill._
import mill.scalalib.TestModule.Utest
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._

object v {
  val scala = "2.12.13"
  val arithmetic = "0.1"
  val chisel3 = ivy"edu.berkeley.cs::chisel3:3.5-SNAPSHOT"
  val chisel3Plugin = ivy"edu.berkeley.cs:::chisel3-plugin:3.5-SNAPSHOT"
  val chiseltest = ivy"edu.berkeley.cs::chiseltest:0.5-SNAPSHOT"
  val utest = ivy"com.lihaoyi::utest:latest.integration"
  val upickle = ivy"com.lihaoyi::upickle:latest.integration"
  val osLib = ivy"com.lihaoyi::os-lib:latest.integration"
  val breeze = ivy"com.github.ktakagaki.breeze::breeze:2.0"
  val breezeNatives = ivy"com.github.ktakagaki.breeze::breeze-natives:2.0"
  val breezeViz = ivy"org.scalanlp::breeze-viz:2.0"
  val spire = ivy"org.typelevel::spire:0.17.0"
  val evilplot = ivy"io.github.cibotech::evilplot:0.8.1"
  //  val prime = ivy"org.apache.commons:commons-math3:3.6.1"
}

object arithmetic extends arithmetic

class arithmetic extends ScalaModule with ScalafmtModule with PublishModule { m =>
  override def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(
      MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
    )
  }

  def scalaVersion = v.scala

  def publishVersion = v.arithmetic

  override def scalacPluginIvyDeps = Agg(v.chisel3Plugin)

  override def ivyDeps = super.ivyDeps() ++ Agg(
    v.chisel3,
    v.chiseltest,
    v.upickle,
    v.osLib,
    v.breeze,
    v.breezeViz,
    v.breezeNatives,
    v.spire,
    v.evilplot
  )

  object tests extends Tests with Utest {
    override def ivyDeps = m.ivyDeps() ++ Agg(v.utest)
  }

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "me.jiuyang",
    url = "https://jiuyang.me",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("sequencer", "arithmetic"),
    developers = Seq(
      Developer("sequencer", "Jiuyang Liu", "https://jiuyang.me/")
    )
  )
}
