#!/bin/bash
# Dev tool: paint the animals' textures and spawn eggs into Fossil Record's resources, render previews, and write the
# dinosaurs' egg models, loot tables, lang and sounds entries. Run ../build.sh all and ../tests/test.sh first.
# Usage: fossil-core/art.sh [preview dir] [dinosaur ids...]   (no ids: all of them)
set -e
cd "$(dirname "$0")/.."
unset JAVA_TOOL_OPTIONS
M=../mod
CP=tests/out/mc-unsigned.jar:$(find $M/cdeps -name '*.jar' -not -name 'mc-26.2.jar' | tr '\n' ':')$(find $M/deps -name '*.jar' -not -path '*/fapi/*' -not -name '26.2-*.jar' -not -name 'fabric-api-*.jar' | tr '\n' ':')
MODS=core/out/classes:fossil-core/out/classes:
RES=fossil-core/src/main/resources
java -Djava.awt.headless=true -cp "tests/out:$MODS$CP" com.seth.fossilrecord.FaunaArt $RES/assets/fossilrecord "${1:-tests/out/fauna-preview}"
java -Djava.awt.headless=true -cp "tests/out:$MODS$CP" com.seth.fossilrecord.DinoArt $RES/assets/fossilrecord "${1:-tests/out/fauna-preview}/dinos" "${@:2}"
java -cp "tests/out:$MODS$CP" com.seth.fossilrecord.DinoGen $RES
