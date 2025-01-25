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
    millDepsHash = "sha256-NYB4g7uCaQZH7MqA7fdQzB2vXUdXfo37MC6hAMaMIZU=";
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
