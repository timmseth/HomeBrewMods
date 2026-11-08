#!/bin/bash
# Compile the tests against every module's classes and run CoreTests (engine, Earth data, Earthish, dinosaurs).
# Run mods/build.sh all first.
set -e
cd "$(dirname "$0")/.."
unset JAVA_TOOL_OPTIONS
M=../mod
CP=$(find $M/cdeps -name '*.jar' | tr '\n' ':')$(find $M/deps -name '*.jar' -not -path '*/fapi/*' -not -name '26.2-*.jar' -not -name 'fabric-api-*.jar' | tr '\n' ':')
MODS=""; for m in core earthdata earthish fossil-core earth-fossils earthish-fossils landmarks-core earth-landmarks earthish-landmarks; do MODS="$MODS$m/out/classes:"; done
mkdir -p tests/out
javac --release 21 -nowarn -Xlint:-options -proc:none -cp "$MODS$CP" -d tests/out $(find tests/src/java -name '*.java')
PYTILE=tests/out/py.etile
python3 - "$PYTILE" <<'PY'
import sys, numpy as np
sys.path.insert(0, '../tools')
import etile
a = np.zeros((1024, 1024), dtype='<i2'); a[0, 0] = 1234; a[1023, 1023] = -4321; a[256, 512] = 77
open(sys.argv[1], 'wb').write(etile.encode('elev', 7, 3, 5, a))
PY
[ -f tests/out/mc-unsigned.jar ] || { cp $M/cdeps/mc-26.2.jar tests/out/mc-unsigned.jar && zip -q -d tests/out/mc-unsigned.jar 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.EC' 'META-INF/*.DSA' 2>/dev/null || true; }
RCP=tests/out/mc-unsigned.jar:$(find $M/cdeps -name '*.jar' -not -name 'mc-26.2.jar' | tr '\n' ':')$(find $M/deps -name '*.jar' -not -path '*/fapi/*' -not -name '26.2-*.jar' -not -name 'fabric-api-*.jar' | tr '\n' ':')
java -cp "tests/out:$MODS$RCP" com.seth.worldbound.CoreTests "$PYTILE"
