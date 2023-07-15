{
  description = "vector";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }@inputs:
    let
      overlay = import ./overlay.nix;
    in
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = import nixpkgs { inherit system; };
          deps = with pkgs; [
            cmake
            zlib

            mill
            python3
            go
            ammonite
            metals
            gnused
            coreutils
            gnumake
            gnugrep
            which
            parallel
            protobuf
            ninja
            verilator
            antlr4
            numactl
            dtc
            circt

            yarn
            mdl
          ];
        in
        {
          legacyPackages = pkgs;
          devShell = pkgs.mkShell { 
            buildInputs = deps;
          };
        }
      )
    // { inherit inputs; };
}
