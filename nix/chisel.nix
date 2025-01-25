{ fetchMillDeps
, publishMillJar
, fetchFromGitHub
, git
}:
let
  chiselSrc = fetchFromGitHub {
    owner = "chipsalliance";
    repo = "chisel";
    rev = "8a1f1b66e5e87dff6c8356fae346eb46512756cf";
    hash = "sha256-pB8kzqUmvHTG2FqRRjqig1FK9pGYrgBDOOekCqkwrsE=";
  };
  chiselDeps = fetchMillDeps {
    name = "chisel";
    src = chiselSrc;
    millDepsHash = "sha256-NBHUq5MaGiiaDA5mjeP0xcU5jNe9wWordL01a6khy7I=";
  };
in
publishMillJar {
  name = "chisel";
  src = chiselSrc;

  publishTargets = [
    "unipublish"
  ];

  buildInputs = [
    chiselDeps.setupHook
  ];

  nativeBuildInputs = [
    # chisel requires git to generate version
    git
  ];

  passthru = {
    inherit chiselDeps;
  };
}
