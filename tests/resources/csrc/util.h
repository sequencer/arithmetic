#pragma once

#include <cstdint>
#include "glog_exception_safe.h"


inline char *get_env_arg(const char *name) {
  char *val = std::getenv(name);
  CHECK_S(val != nullptr) << fmt::format("cannot find environment of name '{}'", name);
  return val;
}

inline char *get_env_arg_default(const char *name, char *default_val) {
  char *val = std::getenv(name);
  return val == nullptr ? default_val : val;
}