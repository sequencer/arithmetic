{ publishMillJar
, git
, callPackage
}:
let
  sources = callPackage ./_sources/generated.nix { };
in
publishMillJar {
  name = "chisel";

  inherit (sources.chisel) src;

  lockFile = ./chisel-mill-lock.nix;

  publishTargets = [
    "unipublish"
  ];

  nativeBuildInputs = [
    # chisel requires git to generate version
    git
  ];
}
