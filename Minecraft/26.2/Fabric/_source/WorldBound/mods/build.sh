#!/bin/bash
# Build one module (or "all"): compile against the Minecraft/Fabric compile-only jars plus the modules it uses,
# copy its resources, and nest the library jars it ships (Fabric jar-in-jar, so a library shared by two mods loads once).
#   mods/build.sh core | earthdata | earthish | fossil-core | earth-fossils | ... | all
set -e
cd "$(dirname "$0")"
unset JAVA_TOOL_OPTIONS
CDEPS=../mod/cdeps
[ -d $CDEPS ] || { echo "no mod/cdeps — run mod/setup-deps.sh <mcdeps.tar> first"; exit 1; }
ORDER="core earthdata earthish fossil-core earth-fossils earthish-fossils landmarks-core earth-landmarks earthish-landmarks tatv"
build() {
  local m=$1
  local meta=$m/module.json
  local uses=$(python3 -c "import json;print(' '.join(json.load(open('$meta')).get('uses',[])))")
  local nest=$(python3 -c "import json;print(' '.join(json.load(open('$meta')).get('nest',[])))")
  local CP=$(find $CDEPS ../mod/lib $m/lib -name '*.jar' 2>/dev/null | tr '\n' ':')$(find ../mod/deps -name '*.jar' -not -path '*/fapi/*' -not -name '26.2-*.jar' -not -name 'fabric-api-*.jar' | tr '\n' ':')
  for u in $uses; do CP="$CP$u/out/classes:"; done
  rm -rf $m/out && mkdir -p $m/out/classes
  find $m/src/main/java -name '*.java' > $m/out/sources.txt
  if [ -s $m/out/sources.txt ]; then
    javac --release 21 -nowarn -Xlint:-options -proc:none -cp "$CP" -d $m/out/classes @$m/out/sources.txt
  fi
  [ -d $m/src/main/resources ] && cp -r $m/src/main/resources/. $m/out/classes/
  local id=$(python3 -c "import json;print(json.load(open('$m/src/main/resources/fabric.mod.json'))['id'])")
  local ver=$(python3 -c "import json;print(json.load(open('$m/src/main/resources/fabric.mod.json'))['version'])")
  if [ -n "$nest" ]; then
    mkdir -p $m/out/classes/META-INF/jars
    for n in $nest; do cp $n/out/*.jar $m/out/classes/META-INF/jars/; done
    python3 - "$m/out/classes/fabric.mod.json" $m/out/classes/META-INF/jars <<'PY'
import json,sys,os
p,d=sys.argv[1],sys.argv[2]
j=json.load(open(p)); j['jars']=[{'file':'META-INF/jars/'+f} for f in sorted(os.listdir(d))]
json.dump(j,open(p,'w'),indent=2)
PY
  fi
  (cd $m/out/classes && zip -q -r ../$id-$ver.jar . -x 'META-INF/jars/*.jar.tmp')
  echo "built $m/out/$id-$ver.jar ($(du -h $m/out/$id-$ver.jar | cut -f1))"
}
if [ "$1" = all ]; then for m in $ORDER; do [ -f $m/module.json ] && build $m; done; else for m in "$@"; do build $m; done; fi
