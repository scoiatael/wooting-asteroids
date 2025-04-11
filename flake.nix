{
  description = "A very basic flake";

  inputs.nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  # TODO: https://flake.parts/getting-started.html
  outputs = { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        lein = pkgs.leiningen.override { jdk = pkgs.temurin-jre-bin-23; };
      in {
        packages.default = lein;
        devShells.default = pkgs.mkShellNoCC { packages = [ lein ]; };
      });
}
