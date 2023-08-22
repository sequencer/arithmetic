import mill._
import mill.scalalib._
import mill.scalalib.publish._
import coursier.maven.MavenRepository

trait HasChisel
  extends ScalaModule {
  // Define these for building chisel from source
  def chiselModule: Option[ScalaModule]

  override def moduleDeps = super.moduleDeps ++ chiselModule

  def chiselPluginJar: T[Option[PathRef]]

  override def scalacOptions = T(super.scalacOptions() ++ chiselPluginJar().map(path => s"-Xplugin:${path.path}"))

  override def scalacPluginClasspath: T[Agg[PathRef]] = T(super.scalacPluginClasspath() ++ chiselPluginJar())

  // Define these for building chisel from ivy
  def chiselIvy: Option[Dep]

  override def ivyDeps = T(super.ivyDeps() ++ chiselIvy)

  def chiselPluginIvy: Option[Dep]

  override def scalacPluginIvyDeps: T[Agg[Dep]] = T(super.scalacPluginIvyDeps() ++ chiselPluginIvy.map(Agg(_)).getOrElse(Agg.empty[Dep]))
}

trait ArithmeticModule
  extends ScalaModule
    with HasChisel {
  def spireIvy: T[Dep]

  def evilplotIvy: T[Dep]

  def oslibIvy: T[Dep]

  override def ivyDeps = T(super.ivyDeps() ++ Seq(spireIvy(), evilplotIvy()))
}

// TODO: migrate test to svsim

trait ArithmeticTestModule
  extends TestModule
    with HasChisel
    with TestModule.ScalaTest {
  def arithmeticModule: ArithmeticModule
  def spireIvy: T[Dep]

  def evilplotIvy: T[Dep]

  def oslibIvy: T[Dep]

  def scalatestIvy: Dep

  def scalaparIvy: Dep

  override def moduleDeps = super.moduleDeps ++ Some(arithmeticModule)

  override def defaultCommandName() = "test"

  override def ivyDeps = T(
    super.ivyDeps() ++ Agg(
      scalatestIvy,
      scalaparIvy,
      spireIvy(),
      evilplotIvy()
    )
  )
}
