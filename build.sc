import mill._
import scalalib._
import scalafmt._
import publish._
import mill.scalalib.TestModule.Utest

import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version_mill0.10:0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

object v {
  val chisel3 = ivy"edu.berkeley.cs::chisel3:3.5.4"
  val chisel3Plugin = ivy"edu.berkeley.cs:::chisel3-plugin:3.5.4"
  val chiseltest = ivy"edu.berkeley.cs::chiseltest:0.5.4"
  val utest = ivy"com.lihaoyi::utest:latest.integration"
  val upickle = ivy"com.lihaoyi::upickle:latest.integration"
  val osLib = ivy"com.lihaoyi::os-lib:latest.integration"
  val bc = ivy"org.bouncycastle:bcprov-jdk15to18:1.71"  
  val spire = ivy"org.typelevel::spire:0.17.0"
  val evilplot = ivy"io.github.cibotech::evilplot:0.8.1"
}

// TODO: add 2.13 after chisel publish to 2.13
object arithmetic extends mill.Cross[arithmeticCrossModule]("2.12.13")

class arithmeticCrossModule(val crossScalaVersion: String) extends CrossScalaModule with ScalafmtModule with PublishModule {
  def chisel3Module: Option[PublishModule] = None

  def chiseltestModule: Option[PublishModule] = None

  override def moduleDeps = Seq() ++ chisel3Module ++ chiseltestModule

  override def scalacPluginIvyDeps = if (chisel3Module.isEmpty) Agg(v.chisel3Plugin) else Agg.empty[Dep]

  override def ivyDeps = Agg(
    v.upickle,
    v.osLib,
    v.spire,
    v.evilplot
  ) ++ (if (chisel3Module.isEmpty) Some(v.chisel3) else None)
    
  object tests extends Tests with Utest with ScalafmtModule {
    override def ivyDeps = super.ivyDeps() ++ Agg(v.utest, v.bc) ++ 
      (if (chiseltestModule.isEmpty) Some(v.chiseltest) else None)

  }

  def publishVersion = de.tobiasroeser.mill.vcs.version.VcsVersion.vcsState().format()

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

  override def sonatypeUri:         String = "https://s01.oss.sonatype.org/service/local"
  override def sonatypeSnapshotUri: String = "https://s01.oss.sonatype.org/content/repositories/snapshots"
  def githubPublish = T {
    os.proc("gpg", "--import", "--no-tty", "--batch", "--yes")
      .call(stdin = java.util.Base64.getDecoder.decode(sys.env("PGP_SECRET").replace("\n", "")))
    val PublishModule.PublishData(artifactInfo, artifacts) = publishArtifacts()
    new SonatypePublisher(
      sonatypeUri,
      sonatypeSnapshotUri,
      s"${sys.env("SONATYPE_USERNAME")}:${sys.env("SONATYPE_PASSWORD")}",
      true,
      Seq(
        s"--passphrase=${sys.env("PGP_PASSPHRASE")}",
        "--no-tty",
        "--pinentry-mode=loopback",
        "--batch",
        "--yes",
        "-a",
        "-b"
      ).flatMap(_.split("[,]")),
      60000,
      5000,
      T.log,
      120000,
      true
    ).publish(artifacts.map { case (a, b) => (a.path, b) }, artifactInfo, true)
  }
}