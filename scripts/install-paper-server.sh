#!/usr/bin/env bash
#
# Installs a Paper server into the local Maven repository so the Minecraft 26.x
# adapters can compile against it.
#
# Since 26.1 the server is no longer obfuscated and Paper ships it with Mojang
# names, so the patched Paper jar is exactly what the adapters link against at
# runtime. This script downloads a Paper build, lets Paperclip patch it, merges
# the server and its libraries into a single jar and installs it as
# io.papermc.paper:paper-server-bundle:<minecraft version>.
#
# Usage: scripts/install-paper-server.sh <minecraft version> [paper build]
#
# Requires Java 25, curl, python3, unzip, zip and mvn.

set -euo pipefail

minecraft_version="${1:?usage: $0 <minecraft version> [paper build]}"
paper_build="${2:-latest}"

work_dir="$(mktemp -d)"
trap 'rm -rf "${work_dir}"' EXIT

api="https://fill.papermc.io/v3/projects/paper/versions/${minecraft_version}/builds/${paper_build}"
download_url="$(curl --fail --silent --show-error --location "${api}" \
  | python3 -c 'import json, sys; print(json.load(sys.stdin)["downloads"]["server:default"]["url"])')"

echo "Downloading ${download_url}"
curl --fail --silent --show-error --location --output "${work_dir}/paper.jar" "${download_url}"

(cd "${work_dir}" && java -Dpaperclip.patchonly=true -jar paper.jar > /dev/null)

server_jar="${work_dir}/versions/${minecraft_version}/paper-${minecraft_version}.jar"
if [[ ! -f "${server_jar}" ]]; then
  echo "Paperclip did not produce ${server_jar}" >&2
  exit 1
fi

bundle_dir="${work_dir}/bundle"
mkdir -p "${bundle_dir}"
while IFS= read -r -d '' library; do
  unzip -qo "${library}" -d "${bundle_dir}"
done < <(find "${work_dir}/libraries" -name '*.jar' -print0)
# The server is extracted last so its classes win over any duplicates
unzip -qo "${server_jar}" -d "${bundle_dir}"
# Signatures and multi-release metadata of the merged jars are meaningless for the bundle
rm -rf "${bundle_dir}/META-INF"

bundle_jar="${work_dir}/paper-server-bundle.jar"
(cd "${bundle_dir}" && zip -qr "${bundle_jar}" .)

mvn --batch-mode --no-transfer-progress install:install-file \
  -Dfile="${bundle_jar}" \
  -DgroupId=io.papermc.paper \
  -DartifactId=paper-server-bundle \
  -Dversion="${minecraft_version}" \
  -Dpackaging=jar \
  -DgeneratePom=true
