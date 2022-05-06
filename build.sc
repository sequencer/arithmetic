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
  val bc = ivy"org.bouncycastle:bcprov-jdk15to18:1.71"  
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
  )

  object tests extends Tests with Utest {
    override def ivyDeps = m.ivyDeps() ++ Agg(v.utest, v.bc)
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
