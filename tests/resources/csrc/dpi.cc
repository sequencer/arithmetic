#ifdef COSIM_VERILATOR

#include <VTestBench__Dpi.h>

#endif

#include <csignal>

#include <fmt/core.h>
#include <glog/logging.h>

#include "svdpi.h"
#include "vbridge_impl.h"

#if VM_TRACE

void VBridgeImpl::dpiDumpWave() {
  ::dpiDumpWave((wave + op + rmstring + ".fst").c_str());
}

#endif

void dpiInitCosim() {
  svSetScope(svGetScopeFromName("TOP.TestBench.verificationModule.verbatim"));
  vbridge_impl_instance.dpiInitCosim();
}

[[maybe_unused]] void dpiTimeoutCheck() {
  vbridge_impl_instance.timeoutCheck();
}

[[maybe_unused]] void dpiPeek(svBit ready) {
  vbridge_impl_instance.dpiPeek(ready);
}

[[maybe_unused]] void dpiPoke(
                 svBit *valid,
                 svBitVecVal *a,
                 svBitVecVal *b,
                 svBit *op,
                 svBitVecVal *rm) {
  vbridge_impl_instance.dpiPoke(DutInterface{valid, a, b, op, rm});
}

[[maybe_unused]] void dpiCheck(
            svBit valid,
            const svBitVecVal *result,
            const svBitVecVal *fflags) {

   vbridge_impl_instance.dpiCheck(valid, *result, *fflags);
}
