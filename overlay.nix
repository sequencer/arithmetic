final: prev: {
  espresso = final.callPackage ./nix/espresso.nix { };
  softfloat = final.callPackage ./nix/softfloat.nix { };
  testfloat = final.callPackage ./nix/testfloat.nix { };
}
