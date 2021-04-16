// import Mill dependency
import mill._
import scalalib._
import scalafmt._
import publish._

val defaultVersions = Map(
  "chisel3" -> "3.4.2",
  "chisel3-plugin" -> "3.4.2",
  "scala" -> "2.12.13",
)

def getVersion(dep: String, org: String = "edu.berkeley.cs", cross: Boolean = false) = {
  val version = sys.env.getOrElse(dep + "Version", defaultVersions(dep))
  if (cross)
    ivy"$org:::$dep:$version"
  else
    ivy"$org::$dep:$version"
}

object arithmetic extends arithmetic

class arithmetic extends ScalaModule with ScalafmtModule with PublishModule { m =>
  def scalaVersion = defaultVersions("scala")

  def publishVersion = "0.1"

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(getVersion("chisel3-plugin", cross = true))

  override def ivyDeps = super.ivyDeps() ++ Agg(
    getVersion("chisel3"),
  )

  object tests extends Tests {
    override def ivyDeps = Agg(ivy"com.lihaoyi::utest:latest.integration")

    def testFrameworks = Seq("utest.runner.Framework")
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
