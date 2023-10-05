package tests

import chisel3.RawModule
import float._

import java.text.SimpleDateFormat
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.parallel.CollectionConverters._
import chisel3.RawModule
import firrtl.AnnotationSeq
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chisel3.experimental.ExtModule
import chisel3.util.{HasExtModuleInline, HasExtModuleResource}
import firrtl.stage.FirrtlCircuitAnnotation

import chisel3.stage._
import os._




trait FMATester extends AnyFlatSpec with Matchers with ParallelTestExecution {
  val roundings = Seq(
    "-rnear_even" -> "0",
    "-rminMag" -> "1",
    "-rmin" -> "2",
    "-rmax" -> "3",
    "-rnear_maxMag" -> "4",
  )

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

  def check(stdouts: Seq[String]) = {
    stdouts foreach (_ shouldNot include("expected"))
    stdouts foreach (_ shouldNot include("Ran 0 tests."))
    stdouts foreach (_ should include("No errors found."))
  }

  def test(name: String, module: () => RawModule, softfloatArg: Seq[String]): Seq[String] = {
    val (softfloatArgs, dutArgs) = (roundings.map { case (s, d) =>
      (Seq(s, "-tininessafter") ++ softfloatArg, Seq(d, "0"))
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

    var topName: String = null
    val emulatorThreads = 8

    val runDir: Path = os.pwd / "run"
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


//    os.remove(rtlDir / "dut.sv")
//    os.write(rtlDir / "dut.sv", chisel3.getVerilogString(new VerificationModule))



    val annos: AnnotationSeq = Seq(
      new chisel3.stage.phases.Elaborate,
      new chisel3.stage.phases.Convert
    ).foldLeft(
      Seq(
        ChiselGeneratorAnnotation(() => new TestBench(8,24))
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


    val allCSourceFiles = Seq(
      "dpi.cc",
      "vbridge_impl.cc",
      "vbridge_impl.h",
      "encoding.h"
    ).map { f =>
      os.pwd / "tests" / "resources" / "csrc" / f
    }

    val allCHeaderFiles = Seq(
      "verilator.h"
    ).map { f =>
      os.pwd / "tests" / "resources" / "includes" / f
    }

    val verilatorArgs = Seq(
      // format: off
      "--x-initial unique",
      "--output-split 100000",
      "--max-num-width 1048576",
      "--timing",
      // use for coverage
      "--coverage-user",
      "--assert",
      // format: on
      "--main"
    )

    os.write(emulatorBuildDir / "CMakeLists.txt",
      // format: off
      s"""cmake_minimum_required(VERSION 3.20)
         |project(emulator)
         |set(CMAKE_CXX_STANDARD 17)
         |
         |find_package(args REQUIRED)
         |find_package(glog REQUIRED)
         |find_package(fmt REQUIRED)
         |find_package(verilator REQUIRED)
         |find_package(Threads REQUIRED)
         |set(THREADS_PREFER_PTHREAD_FLAG ON)
         |
         |add_executable(emulator
         |${allCSourceFiles.mkString("\n")}
         |)
         |
         |target_include_directories(emulator PUBLIC $emulatorCHeader)
         |
         |target_link_libraries(emulator PUBLIC $${CMAKE_THREAD_LIBS_INIT})
         |target_link_libraries(emulator PUBLIC  fmt::fmt glog::glog )  # note that libargs is header only, nothing to link
         |target_compile_definitions(emulator PRIVATE COSIM_VERILATOR)
         |
         |verilate(emulator
         |  SOURCES
         |  ${verilogs.mkString("\n")}
         |  "TRACE_FST"
         |  TOP_MODULE $topName
         |  PREFIX V$topName
         |  OPT_FAST
         |  THREADS $emulatorThreads
         |  VERILATOR_ARGS ${verilatorArgs.mkString(" ")}
         |)
         |""".stripMargin
      // format: on
    )

    // build verilator
    os.proc(Seq(
      "cmake",
      "-G", "Ninja",
      "-S", emulatorBuildDir,
      "-B", emulatorBuildDir
    ).map(_.toString)).call(emulatorBuildDir)

    // build emulator
    os.proc(Seq("ninja", "-C", emulatorBuildDir).map(_.toString)).call(emulatorBuildDir)

    // run
    os.proc(Seq("./emulator").map(_.toString)).call(emulatorBuildDir)


    Seq("No errors found.")
  }
}

class DivSqrtRecFn_smallSpec extends FMATester {
  def test(f: Int, fn: String): Seq[String] = {
    def generator(options: Int) = fn match {
      case "div" => () => new TestBench(exp(f), sig(f))
      case "sqrt" => () => new TestBench(exp(f), sig(f))
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


}