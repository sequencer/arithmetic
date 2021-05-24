// import Mill dependency
import mill._
import scalalib._
import scalafmt._
import publish._
import coursier.maven.MavenRepository

val defaultVersions = Map(
  "chisel3" -> "3.5-SNAPSHOT",
  "chisel3-plugin" -> "3.5-SNAPSHOT",
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
  override def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(
      MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
    )
  }

  def scalaVersion = defaultVersions("scala")

  def publishVersion = "0.1"

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(getVersion("chisel3-plugin", cross = true))

  override def ivyDeps = super.ivyDeps() ++ Agg(
    getVersion("chisel3"),
    ivy"com.lihaoyi::upickle:latest.integration",
    ivy"com.lihaoyi::os-lib:latest.integration",
  )

  object tests extends Tests {
    override def ivyDeps = Agg(ivy"com.lihaoyi::utest:latest.integration")

    def testFramework = "utest.runner.Framework"
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
