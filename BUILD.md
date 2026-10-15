# Build introduction

The project is built by the [GitHub Actions workflow](.github/workflows/build.yml).
It installs the required remapped Spigot NMS artifacts with BuildTools for
Minecraft 1.20.x–1.21.x, installs the Paper servers the 26.x adapters compile
against, and then packages the cross-version plugin jar.

Minecraft 26.x is no longer obfuscated and Paper runs it with Mojang names, so
the 26.x adapters compile directly against the Paper server instead of Spigot,
whose internals differ from Paper's. To build locally, install them first:

```shell
scripts/install-paper-server.sh 26.1.2 74
scripts/install-paper-server.sh 26.2 134
scripts/install-paper-server.sh 26.3 193
```

The workflow uses Java 25 because Minecraft 26.x requires it. Plugin bytecode
continues to target Java 21 so the single cross-version jar remains loadable on
the older supported servers.

Run the workflow from the Actions tab with **Run workflow**, or push a commit to
the `main` or `master` branch. The completed run publishes the plugin jar as
the `fakeplayer` workflow artifact.
