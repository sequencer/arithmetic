{ lib
, publishMillJar
, chisel
}:
publishMillJar {
  name = "arithmetic";

  src = with lib.fileset; toSource {
    fileset = unions [
      ../build.mill
      ../common.mill
      ../arithmetic
    ];
    root = ../.;
  };

  buildInputs = [
    chisel.setupHook
  ];

  lockFile = ./arithmetic-mill-lock.nix;

  publishTargets = [
    "arithmetic[snapshot]"
  ];
}
