final: prev: {
  mill = let jre = final.jdk21; in
    (prev.mill.override { inherit jre; }).overrideAttrs rec {
      # Fixed the buggy sorting issue in target resolve
      version = "0.12.8-1-46e216";
      src = final.fetchurl {
        url = "https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/${version}/mill-dist-${version}-assembly.jar";
        hash = "sha256-XNtl9NBQPlkYu/odrR/Z7hk3F01B6Rk4+r/8tMWzMm8=";
      };
      passthru = { inherit jre; };
    };

  espresso = final.callPackage ./nix/espresso.nix { };
  softfloat = final.callPackage ./nix/softfloat.nix { };
  testfloat = final.callPackage ./nix/testfloat.nix { };

  submodules = final.callPackage ./nix/_sources/generated.nix { };
  chisel = final.callPackage ./nix/chisel.nix { };
  arithmetic = final.callPackage ./nix/arithmetic.nix { };
  bump-arithmetic = final.callPackage ./nix/bump-arithmetic.nix { };
}
