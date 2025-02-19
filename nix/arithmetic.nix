{ lib
, fetchMillDeps
, publishMillJar
, chisel
}:
let
  arithmeticSrc = with lib.fileset; toSource {
    fileset = unions [
      ../build.mill
      ../common.mill
      ../arithmetic
    ];
    root = ../.;
  };
  arithmeticDeps = fetchMillDeps {
    name = "arithmetic";
    src = arithmeticSrc;
    buildInputs = [ chisel.setupHook ];
    millDepsHash = "sha256-lv1d+NSXsV42Xb1NuVZjlz07T6W4Xqkx/3MQvaVqRTA=";
  };
in
publishMillJar {
  name = "arithmetic";

  src = arithmeticSrc;

  buildInputs = [
    arithmeticDeps.setupHook
    chisel.setupHook
  ];

  publishTargets = [
    "arithmetic[snapshot]"
  ];

  passthru = {
    inherit arithmeticDeps;
  };
}
