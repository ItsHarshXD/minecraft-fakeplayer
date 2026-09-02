# Build introduction

The project is built by the [GitHub Actions workflow](.github/workflows/build.yml).
It installs the required remapped Spigot NMS artifacts with BuildTools and then
packages the cross-version plugin jar.

The workflow uses Java 25 because Minecraft 26.x requires it. Plugin bytecode
continues to target Java 21 so the single cross-version jar remains loadable on
the older supported servers.

Run the workflow from the Actions tab with **Run workflow**, or push a commit to
the `main` or `master` branch. The completed run publishes the plugin jar as
the `fakeplayer` workflow artifact.
