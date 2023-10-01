#pragma once

#include <optional>
#include <queue>

#include <VTestBench__Dpi.h>
#include "verilated_fst_c.h"


#include <svdpi.h>



class VBridgeImpl {
public:
    explicit VBridgeImpl();

    void dpiDumpWave();

    void dpiInitCosim();

    uint64_t get_t();

    int timeoutCheck();

    uint64_t getCycle() { return ctx->time(); }




private:

    VerilatedContext *ctx;
    VerilatedFstC tfp;

    uint64_t _cycles;


    const std::string wave = "/home/yyq/Projects/arithmetic/run/wave";




};

extern VBridgeImpl vbridge_impl_instance;
