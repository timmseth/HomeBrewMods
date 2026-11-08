#!/bin/bash
# One-time: unpack the Minecraft/Fabric dependency bundle and make compile-only (class-version-lowered) copies.
# mcdeps.tar holds the Minecraft 26.2 jar, Fabric Loader and its libraries, and Fabric API; see BUILDING.md.
# Usage: ./setup-deps.sh /path/to/mcdeps.tar
set -e
cd "$(dirname "$0")"
unset JAVA_TOOL_OPTIONS
TAR=${1:?path to mcdeps.tar}
rm -rf deps cdeps && mkdir -p deps cdeps deps/fapi
tar -xf "$TAR" -C deps
(cd deps/fapi && unzip -q -o ../fabric-api-*.jar 'META-INF/jars/*' && mv META-INF/jars/*.jar . && rm -rf META-INF)
python3 patchver.py deps/26.2-*.jar cdeps/mc-26.2.jar
python3 patchver.py deps/net/fabricmc/fabric-loader/*/fabric-loader-*.jar cdeps/fabric-loader.jar
for j in deps/fapi/*.jar; do python3 patchver.py "$j" "cdeps/$(basename "$j")"; done
echo "deps ready: $(ls cdeps | wc -l) compile jars"
