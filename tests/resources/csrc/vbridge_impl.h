#pragma once

#include <optional>
#include <queue>

#include <VTestBench__Dpi.h>
#include "verilated_fst_c.h"


#include <svdpi.h>

#include "encoding.h"

#include <cstdio>
#include <cassert>
#include <cstdint>

extern "C" {
#include "functions.h"
#include "softfloat.h"
#include "genCases.h"
#include "genLoops.h"
}

struct testdata {
    uint64_t a;
    uint64_t b;
    uint64_t expected_out;
    function_t function;
    roundingMode_t roundingMode;
    exceptionFlag_t expectedException;
};


class VBridgeImpl {
public:
    explicit VBridgeImpl();

    void dpiDumpWave();

    void dpiInitCosim();

    uint64_t get_t();

    int timeoutCheck();

    uint64_t getCycle() { return ctx->time(); }

    void dpiBasePoke(uint32_t *a);

    void dpiPeekPoke(const DutInterface &toDut);

    void dpiBasePeek(svBit ready);

    std::queue <testdata> test_queue;

    testdata testcase;

    void initTestCases();

    void dpiCheck(svBit valid,
                  svBitVecVal result,
                  svBitVecVal fflags);

    void set_available();

    void clr_available();

    void reloadcase();

    uint64_t cnt;


private:

    VerilatedContext *ctx;
    VerilatedFstC tfp;

    uint64_t _cycles;


    const std::string wave = "/home/yyq/Projects/arithmetic/run/wave";

    bool available;






};

extern VBridgeImpl vbridge_impl_instance;
