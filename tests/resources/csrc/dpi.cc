#ifdef COSIM_VERILATOR
#include <VTestBench__Dpi.h>
#endif

#include <csignal>

#include <glog/logging.h>
#include <fmt/core.h>

#include "svdpi.h"
#include "vbridge_impl.h"
#include "encoding.h"


//void sigint_handler(int s) {
//  terminated = true;
//  dpiFinish();
//}


#if VM_TRACE

void VBridgeImpl::dpiDumpWave() {

        ::dpiDumpWave((wave + op + rmstring + ".fst").c_str());

}
#endif

void dpiInitCosim() {
//  std::signal(SIGINT, sigint_handler);
  svSetScope(svGetScopeFromName("TOP.TestBench.verificationModule.verbatim"));
  vbridge_impl_instance.dpiInitCosim();
}

[[maybe_unused]] void dpiTimeoutCheck() {

        vbridge_impl_instance.timeoutCheck();

}

[[maybe_unused]] void dpiBasePoke(svBitVecVal *a) {
  vbridge_impl_instance.dpiBasePoke(a);
}

[[maybe_unused]] void dpiBasePeek(svBit ready) {
  vbridge_impl_instance.dpiBasePeek(ready);
}

[[maybe_unused]] void dpiPeekPoke(
                 svBit *valid,
                 svBitVecVal *a,
                 svBitVecVal *b,
                 svBitVecVal *op,
                 svBitVecVal *rm) {
  vbridge_impl_instance.dpiPeekPoke(DutInterface{valid, a, b, op, rm});

}

[[maybe_unused]] void dpiCheck(
            svBit valid,
            const svBitVecVal *result,
            const svBitVecVal *fflags) {

   vbridge_impl_instance.dpiCheck(valid, *result, *fflags);
}





