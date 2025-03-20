{ writeShellApplication
, mill
, mill-ivy-fetcher
, submodules
}:
writeShellApplication {
  name = "bump-arithmetic";

  runtimeInputs = [
    mill-ivy-fetcher
    mill
  ];

  text = ''
    chiselDir=$(mktemp -d -t 'chisel_src_XXX')
    cp -rT ${submodules.chisel.src} "$chiselDir"/chisel
    chmod -R u+w "$chiselDir"/chisel

    mif run -p "$chiselDir"/chisel -o ./nix/chisel-mill-lock.nix

    ivyLocal=$(nix build '.#chisel' --no-link --print-out-paths -L -j auto)
    export JAVA_TOOL_OPTIONS="''${JAVA_TOOL_OPTIONS:-} -Dcoursier.ivy.home=$ivyLocal -Divy.home=$ivyLocal"
    mif run -o ./nix/arithmetic-mill-lock.nix
  '';
}
