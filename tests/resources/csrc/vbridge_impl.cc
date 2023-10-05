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

void VBridgeImpl::set_available() {
  available = true;
}

void VBridgeImpl::clr_available() {
  available = false;
}

void VBridgeImpl::dpiInitCosim() {
  google::InitGoogleLogging("emulator");
  FLAGS_logtostderr = true;

  ctx = Verilated::threadContextp();

  LOG(INFO) << fmt::format("[{}] dpiInitCosim", getCycle());
  LOG(INFO) << fmt::format(" running");

  initTestCases();

  dpiDumpWave();
}

void VBridgeImpl::dpiBasePoke(svBitVecVal *a) {
  uint32_t v = 0x1000;
  *a = v;
}

void VBridgeImpl::dpiBasePeek(svBit ready) {

    if(ready == 1) {
      set_available();
      LOG(INFO) << fmt::format("available = {}",available);
    }


}

void VBridgeImpl::dpiPeekPoke(const DutInterface &toDut) {
  if(available==false) return;

  LOG(INFO) << fmt::format("start to poke");

  *toDut.a = test_queue.front().a;
  *toDut.b = test_queue.front().b;
  *toDut.op = 0;
  *toDut.rm = test_queue.front().roundingMode;
  *toDut.refOut = test_queue.front().expected_out;
  *toDut.refFlags = test_queue.front().expectedException;
  *toDut.valid = true;

  test_queue.pop();
}

std::vector<testdata> mygen_abz_f32( float32_t trueFunction( float32_t, float32_t ) , function_t function, roundingMode_t roundingMode) {
  // modified from berkeley-testfloat-3/source/genLoops.c
  union ui32_f32 { uint32_t ui; float32_t f; } u;
  uint_fast8_t trueFlags;

  std::vector<testdata> res;

  genCases_f32_ab_init();
  while ( ! genCases_done ) {
    genCases_f32_ab_next();

    testdata curData;
    curData.function = function;
    curData.roundingMode = roundingMode;
    u.f = genCases_f32_a;
    curData.a = u.ui;
    u.f = genCases_f32_b;
    curData.b = u.ui;
    softfloat_exceptionFlags = 0;
    u.f = trueFunction( genCases_f32_a, genCases_f32_b );
    curData.expectedException = static_cast<exceptionFlag_t>(softfloat_exceptionFlags);
    curData.expected_out = u.ui;

    res.push_back(curData);
  }

  return res;
}


std::vector<testdata> genTestCase(function_t function, roundingMode_t roundingMode) { // call it in dpiInit
  // see berkeley-testfloat-3/source/testfloat_gen.c
  std::vector<testdata> res;

  genCases_setLevel( 1 );

  switch (function) {
    case F32_DIV:
      res = mygen_abz_f32(f32_add, function, roundingMode);
      break;
    default:
      assert(false);
  }

  return res;
}

void outputTestCases(std::vector<testdata> cases) {
  for (auto x : cases) {
    printf("%08x %08x %08x %02x\n", x.a, x.b, x.expected_out, x.expectedException);
  }
}

void fillTestQueue(std::vector<testdata> cases) {
  for (auto x : cases) {
    vbridge_impl_instance.test_queue.push(x);
  }
}

void VBridgeImpl::initTestCases() {
  auto res = genTestCase(F32_DIV, ROUND_NEAR_EVEN);
  fillTestQueue(res);
//  outputTestCases(res); // TODO: demo, please delete
}



VBridgeImpl vbridge_impl_instance;




