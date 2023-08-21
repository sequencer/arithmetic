package float

object Ftests extends App{

  import chisel3.stage.ChiselGeneratorAnnotation
  import firrtl.AnnotationSeq

  val resources = os.resource()
  val runDir = os.pwd / "run"
  os.remove.all(runDir)
  val elaborateDir = runDir / "elaborate"
  os.makeDir.all(elaborateDir)
  val rtlDir = runDir / "rtl"
  os.makeDir.all(rtlDir)
  val emulatorDir = runDir / "emulator"
  os.makeDir.all(emulatorDir)
  val emulatorCSrc = emulatorDir / "src"
  os.makeDir.all(emulatorCSrc)
  val emulatorCHeader = emulatorDir / "include"
  os.makeDir.all(emulatorCHeader)
  val emulatorBuildDir = emulatorDir / "build"
  os.makeDir.all(emulatorBuildDir)

  val emulatorThreads = 8
  val verilatorArgs = Seq(
    // format: off
    "--x-initial unique",
    "--output-split 100000",
    "--max-num-width 1048576",
    "--main",
    "--timing",
    // use for coverage
    "--coverage-user",
    "--assert",
    // format: on
  )

  // TODO: this will be replaced by binder API
  // elaborate
  var topName: String = null
  val annos: AnnotationSeq = Seq(
    new chisel3.stage.phases.Elaborate,
    new chisel3.stage.phases.Convert
  ).foldLeft(
    Seq(
      ChiselGeneratorAnnotation(() => new DivSqrt(8,24))
    ): AnnotationSeq
  ) { case (annos, stage) => stage.transform(annos) }
    .flatMap {
      case FirrtlCircuitAnnotation(circuit) =>
        topName = circuit.main
        os.write.over(elaborateDir / s"$topName.fir", circuit.serialize)
        None
      case _: chisel3.stage.DesignAnnotation[_] => None
      case _: chisel3.stage.ChiselCircuitAnnotation => None
      case a => Some(a)
    }
  os.write.over(elaborateDir / s"$topName.anno.json", firrtl.annotations.JsonProtocol.serialize(annos))

  // rtl
  os.proc(
    "firtool",
    elaborateDir / s"$topName.fir", s"--annotation-file=${elaborateDir / s"$topName.anno.json"}",
    "-dedup",
    "-O=release",
    "--disable-all-randomization",
    "--split-verilog",
    "--preserve-values=none",
    "--preserve-aggregate=all",
    "--strip-debug-info",
    s"-o=$rtlDir"
  ).call()
  val verilogs = os.read.lines(rtlDir / "filelist.f")
    .map(str =>
      try {
        os.Path(str)
      } catch {
        case e: IllegalArgumentException if e.getMessage.contains("is not an absolute path") =>
          rtlDir / str.stripPrefix("./")
      }
    )
    .filter(p => p.ext == "v" || p.ext == "sv")

}