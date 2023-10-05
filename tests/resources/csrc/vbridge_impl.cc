#include <fmt/core.h>
#include <glog/logging.h>

#include "verilated.h"

#include "vbridge_impl.h"



VBridgeImpl::VBridgeImpl() : _cycles(100) {}


uint64_t VBridgeImpl::get_t() {
  return getCycle();
}


int VBridgeImpl::timeoutCheck() {
  if (get_t() > _cycles) {
    LOG(INFO) << fmt::format("Simulation timeout, t={}", get_t());
    dpiFinish();
  }
  return 0;
}

void VBridgeImpl::dpiInitCosim() {
  google::InitGoogleLogging("emulator");
  FLAGS_logtostderr = true;

  ctx = Verilated::threadContextp();


  LOG(INFO) << fmt::format("[{}] dpiInitCosim", getCycle());
  LOG(INFO) << fmt::format(" running");

  dpiDumpWave();
}

void VBridgeImpl::dpiBasePoke(svBitVecVal *a) {
  uint32_t v = 0x1000;
  *a = v;
}

void VBridgeImpl::dpiBasePeek(svBit ready) {
    LOG(INFO) << fmt::format("dpiPeek running = {}", ready);
}

void VBridgeImpl::dpiPeekPoke(const DutInterface &toDut) {
  uint32_t v = 0x1000;
  *toDut.a = v;
  *toDut.b = v;
  *toDut.op = 0;
  *toDut.rm = 0;
  *toDut.refOut = v;
  *toDut.refFlags = v;
  *toDut.valid = 0;
}



VBridgeImpl vbridge_impl_instance;



