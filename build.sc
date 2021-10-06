// import Mill dependency
import mill._
import scalalib._
import scalafmt._
import publish._
import coursier.maven.MavenRepository
import mill.scalalib.TestModule.Utest

object v {
  val scala = "2.12.13"
  val arithmetic = "0.1"
  val chisel3 = ivy"edu.berkeley.cs::chisel3:3.5-SNAPSHOT"
  val chisel3Plugin = ivy"edu.berkeley.cs:::chisel3-plugin:3.5-SNAPSHOT"
  val chiseltest = ivy"edu.berkeley.cs::chiseltest:0.5-SNAPSHOT"
  val utest = ivy"com.lihaoyi::utest:latest.integration"
  val upickle = ivy"com.lihaoyi::upickle:latest.integration"
  val osLib = ivy"com.lihaoyi::os-lib:latest.integration"
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
