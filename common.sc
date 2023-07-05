import mill._
import mill.scalalib._
import mill.scalalib.publish._
import coursier.maven.MavenRepository

import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

trait ArithmeticModule extends ScalaModule with PublishModule {
  // override to build from source, see the usage of chipsalliance/playground
  def chiselModule: Option[PublishModule] = None
  def chiselPluginJar: T[Option[PathRef]] = T(None)

  // override to use chisel from ivy
  def chiselIvyDep: T[Option[Dep]] = None
  def chiselPluginIvyDep: T[Option[Dep]] = None

  // dependencies below is not managed by the Chisel team.
  def spire: T[Dep]
  def evilplot: T[Dep]
  
  override def moduleDeps = Seq() ++ chiselModule
  override def scalacPluginClasspath = T(super.scalacPluginClasspath() ++ chiselPluginJar())
  override def scalacPluginIvyDeps = T(Agg() ++ chiselPluginIvyDep())
  override def scalacOptions = T(super.scalacOptions() ++ chiselPluginJar().map(path => s"-Xplugin:${path.path}"))

  override def ivyDeps = T { 
    Agg(
      spire(),
      evilplot()
    ) ++ 
      chiselIvyDep()
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
}
