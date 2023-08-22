package tests

import chisel3.RawModule
import float._

import java.text.SimpleDateFormat
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.parallel.CollectionConverters._

import chisel3.RawModule
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.parallel.CollectionConverters._

//object Ftests extends App{
//  import chisel3.stage.ChiselGeneratorAnnotation
//  import firrtl.AnnotationSeq
//  import firrtl.stage._
//
//  println("this is Ftests")
//
//  val resources = os.resource()
//  val runDir = os.pwd / "run"
//  os.remove.all(runDir)
//  val elaborateDir = runDir / "elaborate"
//  os.makeDir.all(elaborateDir)
//  val rtlDir = runDir / "rtl"
//  os.makeDir.all(rtlDir)
//  val emulatorDir = runDir / "emulator"
//  os.makeDir.all(emulatorDir)
//  val emulatorCSrc = emulatorDir / "src"
//  os.makeDir.all(emulatorCSrc)
//  val emulatorCHeader = emulatorDir / "include"
//  os.makeDir.all(emulatorCHeader)
//  val emulatorBuildDir = emulatorDir / "build"
//  os.makeDir.all(emulatorBuildDir)
//
//  val emulatorThreads = 8
//  val verilatorArgs = Seq(
//    // format: off
//    "--x-initial unique",
//    "--output-split 100000",
//    "--max-num-width 1048576",
//    "--main",
//    "--timing",
//    // use for coverage
//    "--coverage-user",
//    "--assert",
//    // format: on
//  )
//
//  // TODO: this will be replaced by binder API
//  // elaborate
//  var topName: String = null
//  val annos: AnnotationSeq = Seq(
//    new chisel3.stage.phases.Elaborate,
//    new chisel3.stage.phases.Convert
//  ).foldLeft(
//    Seq(
//      ChiselGeneratorAnnotation(() => new ValExec_DivSqrtRecFN_small_div(8,24,0))
//    ): AnnotationSeq
//  ) { case (annos, stage) => stage.transform(annos) }
//    .flatMap {
//      case FirrtlCircuitAnnotation(circuit) =>
//        topName = circuit.main
//        os.write.over(elaborateDir / s"$topName.fir", circuit.serialize)
//        None
//      case _: chisel3.stage.DesignAnnotation[_] => None
//      case _: chisel3.stage.ChiselCircuitAnnotation => None
//      case a => Some(a)
//    }
//  os.write.over(elaborateDir / s"$topName.anno.json", firrtl.annotations.JsonProtocol.serialize(annos))
//
//  // rtl
//  os.proc(
//    "firtool",
//    elaborateDir / s"$topName.fir", s"--annotation-file=${elaborateDir / s"$topName.anno.json"}",
//    "-dedup",
//    "-O=release",
//    "--disable-all-randomization",
//    "--split-verilog",
//    "--preserve-values=none",
//    "--preserve-aggregate=all",
//    "--strip-debug-info",
//    s"-o=$rtlDir"
//  ).call()
//  val verilogs = os.read.lines(rtlDir / "filelist.f")
//    .map(str =>
//      try {
//        os.Path(str)
//      } catch {
//        case e: IllegalArgumentException if e.getMessage.contains("is not an absolute path") =>
//          rtlDir / str.stripPrefix("./")
//      }
//    )
//    .filter(p => p.ext == "v" || p.ext == "sv")
//
////  os.write(rtlDir / "dut.v", chisel3.getVerilogString(new DivSqrt(8,24)))
//
//}

trait FMATester extends AnyFlatSpec with Matchers with ParallelTestExecution {
  def exp(f: Int) = f match {
    case 16 => 5
    case 32 => 8
    case 64 => 11
  }

  def sig(f: Int) = f match {
    case 16 => 11
    case 32 => 24
    case 64 => 53
  }

  val roundings = Seq(
    "-rnear_even" -> "0",
    "-rminMag" -> "1",
    "-rmin" -> "2",
    "-rmax" -> "3",
    "-rnear_maxMag" -> "4",
    "-rodd" -> "6"
  )

  def check(stdouts: Seq[String]) = {
    stdouts foreach (_ shouldNot include("expected"))
    stdouts foreach (_ shouldNot include("Ran 0 tests."))
    stdouts foreach (_ should include("No errors found."))
  }

  def test(name: String, module: () => RawModule, softfloatArg: Seq[String]): Seq[String] = {
    val (softfloatArgs, dutArgs) = (roundings.map { case (s, d) =>
      (Seq(s, "-tininessbefore") ++ softfloatArg, Seq(d, "0"))
    }).unzip
    test(name, module, "test.cpp", softfloatArgs, Some(dutArgs))
  }

  /** Run a FMA test. Before running, `softfloat_gen` should be accessible in the $PATH environment.
    *
    * @param name          is name of this test, which should corresponds to header's name in `includes` directory.
    * @param module        function to generate DUT.
    * @param harness       C++ harness name, which should corresponds to c++ hardness's name in `csrc` directory.
    * @param softfloatArgs arguments passed to `softfloat_gen` application. If has multiple command lines, multiple test will be executed.
    * @param dutArgs       arguments passed to verilator dut executor, If set to [[None]], no arguments will be passed to.
    */
  def test(name: String, module: () => RawModule, harness: String, softfloatArgs: Seq[Seq[String]], dutArgs: Option[Seq[Seq[String]]] = None) = {

    val testRunDir = os.pwd / "test_run_dir" / s"${this.getClass.getSimpleName}_$name" / s"${new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)}"
    os.makeDir.all(testRunDir)
    os.write(testRunDir / "dut.v", chisel3.getVerilogString(module()))

    /* command Synthesis verilog to C++. */
    val verilatorCompile: Seq[String] = Seq(
      "verilator",
      "-cc",
      "--prefix", "dut",
      "--Mdir", testRunDir.toString,
      "-CFLAGS", s"""-I${getClass.getResource("/includes/").getPath} -include ${getClass.getResource(s"/includes/$name.h").getPath}""",
      "dut.v",
      "--exe", s"${getClass.getResource(s"/csrc/$harness").getPath}",
      "--trace"
    )
    os.proc(verilatorCompile).call(testRunDir)

    /* Build C++ executor. */
    val verilatorBuild: Seq[String] = Seq(
      "make",
      "-C", testRunDir.toString,
      "-j",
      "-f", s"dut.mk",
      "dut")
    os.proc(verilatorBuild).call(testRunDir)

    def executeAndLog(softfloatArg: Seq[String], dutArg: Seq[String]): String = {
      val stdoutFile = testRunDir / s"${name}__${(softfloatArg ++ dutArg).mkString("_")}.txt"
      val vcdFile = testRunDir / s"${name}__${(softfloatArg ++ dutArg).mkString("_")}.vcd"
      os.proc((testRunDir / "dut").toString +: dutArg).call(stdin = os.proc("testfloat_gen" +: softfloatArg).spawn().stdout, stdout = stdoutFile, stderr = vcdFile)
      os.read(stdoutFile)
    }

    (if (dutArgs.isDefined) {
      require(softfloatArgs.size == dutArgs.get.size, "size of softfloatArgs and dutArgs should be same.")
      (softfloatArgs zip dutArgs.get).par.map { case (s, d) => executeAndLog(s, d) }
    } else softfloatArgs.par.map { s => executeAndLog(s, Seq.empty) }).seq
  }
}

class DivSqrtRecFn_smallSpec extends FMATester {
  def test(f: Int, fn: String): Seq[String] = {
    def generator(options: Int) = fn match {
      case "div" => () => new ValExec_DivSqrtRecFN_small_div(exp(f), sig(f))
      case "sqrt" => () => new ValExec_DivSqrtRecFN_small_sqrt(exp(f), sig(f))
    }
    test(
      s"DivSqrtRecF${f}_small_${fn}",
      generator(0),
      (if (fn == "sqrt") Seq("-level2") else Seq.empty) ++ Seq(s"f${f}_${fn}")
    )

  }

  "DivSqrtRecF32_small_div" should "pass" in {
    check(test(32, "div"))
  }

  "DivSqrtRecF32_small_sqrt" should "pass" in {
    check(test(32, "sqrt"))
  }

}