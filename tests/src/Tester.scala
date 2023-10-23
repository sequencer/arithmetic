package tests

import chisel3.RawModule
import float._


import firrtl.AnnotationSeq


import firrtl.stage.FirrtlCircuitAnnotation

import chisel3.stage._
import os._




object Tester extends App {
    var topName: String = "TestBench"
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


    os.proc(
      "make",
      "softfloat",
    ).call()

    os.proc(
      "make",
      "testfloat",
    ).call()

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
      "-O=debug",
      "--split-verilog",
      "--preserve-values=named",
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
      "util.h"
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
         |target_include_directories(emulator PUBLIC
         |$emulatorCHeader
         |${os.pwd}/berkeley-testfloat-3/source
         |${os.pwd}/berkeley-softfloat-3/source/include
         |)
         |
         |target_link_libraries(emulator PUBLIC $${CMAKE_THREAD_LIBS_INIT})
         |target_link_libraries(emulator PUBLIC
         |fmt::fmt
         |glog::glog
         |$runDir/softfloat.a
         |$runDir/testfloat.a
         |)  # note that libargs is header only, nothing to link
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
  for (x <- 0 to 4) {
    val rmMaps = Map(
      0 -> "RNE",
      1 -> "RTZ",
      2 -> "RDN",
      3 -> "RUP",
      4 -> "RMM"
    )
    val runEnv = Map(
      "wave" -> s"${runDir}/",
      "op" -> "sqrt",
      "rm" -> s"$x"
    )
    os.proc(Seq("./emulator").map(_.toString)).call(stdout = runDir / s"${rmMaps(x)}.log", cwd = emulatorBuildDir, env = runEnv)
  }

}