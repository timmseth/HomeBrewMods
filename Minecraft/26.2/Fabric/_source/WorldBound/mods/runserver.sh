#!/bin/bash
# Headless server with a chosen set of the split mods, on a chosen world type, running console commands.
#   SCN=name MODS="core earthdata" LEVEL=worldbound:earth [FRESH=1] mods/runserver.sh "cmd" "cmd" ...
# (modules listed by folder; their built jars are used; core is nested, so listing it is only needed alone)
cd "$(dirname "$0")"
unset JAVA_TOOL_OPTIONS
J=${JAVA25:-$(python3 -c "import jdk4py,os;print(os.path.join(os.path.dirname(jdk4py.__file__),'java-runtime','bin','java'))" 2>/dev/null)}
[ -x "$J" ] || { echo "no Java 25 runtime (set JAVA25=...)"; exit 1; }
D=$(cd ../mod/deps && pwd)
CP=$(find $D -name '*.jar' -not -path "$D/fapi/*" -not -name '26.2-*.jar' -not -name 'fabric-api-*.jar' | tr '\n' ':')
R=run/${SCN:-default}
mkdir -p $R/mods $R/config && rm -f $R/mods/*.jar && cp $D/fabric-api-*.jar $R/mods/
for m in ${MODS:-earthdata}; do cp $m/out/*.jar $R/mods/; done
for j in ${EXTRA_MODS:-}; do cp "$j" $R/mods/; done
[ -e $R/worldbound ] || ln -s $(cd ../mod/run && pwd)/worldbound $R/worldbound
[ -n "$FRESH" ] && rm -rf $R/world
cd $R && echo eula=true > eula.txt
printf "online-mode=false\nlevel-type=${LEVEL:-worldbound\\\\:earth}\nmax-tick-time=-1\npause-when-empty-seconds=0\nspawn-protection=0\nview-distance=4\nsimulation-distance=4\nlevel-name=world\n" > server.properties
( sleep ${BOOT:-40}; for c in "$@"; do echo "$c"; sleep ${STEP:-3}; done; sleep ${TAIL:-2}; echo stop ) | timeout ${LIMIT:-240} $J ${JOPTS:-} -Xmx${XMX:-3G} -Dfabric.gameJarPath=$D/26.2-0.19.5.jar -cp "$CP" net.fabricmc.loader.impl.launch.knot.KnotServer nogui > server.log 2>&1
echo "exit $?"
