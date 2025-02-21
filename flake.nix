{
  description = "arithmetic";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    chisel-nix.url = "github:chipsalliance/chisel-nix";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, chisel-nix, flake-utils }@inputs:
    let
      overlay = import ./overlay.nix;
    in
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = import nixpkgs { inherit system; overlays = [ chisel-nix.overlays.mill-flows overlay ]; };
          deps = with pkgs; [
            git
            mill
            circt
            verilator
            testfloat
            cmake
            libargs
            glog
            fmt
            zlib
            ninja
          ];
        in
        {
          legacyPackages = pkgs;
          devShell = pkgs.mkShell {
            buildInputs = deps;
            env = {
              "SOFT_FLOAT_LIB_DIR" = "${pkgs.softfloat}";
              "TEST_FLOAT_LIB_DIR" = "${pkgs.testfloat}";
            };
          };
          packages.softfloat = pkgs.softfloat;
          packages.testfloat = pkgs.testfloat;
        }
      )
    // { inherit inputs; overlays.default = overlay; };
}
