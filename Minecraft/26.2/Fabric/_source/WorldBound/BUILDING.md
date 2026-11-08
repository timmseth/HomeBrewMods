# Building

No Gradle: small shell scripts compile against the real Minecraft and Fabric jars with `javac`. Linux, macOS or
WSL with bash, Python 3, `zip`/`unzip`, and a JDK (any JDK that can compile with `--release 21`; the game itself runs
on Java 25).

## 1. The game jars (`mcdeps.tar`)

The build needs, in one tar file:

- the Minecraft **26.2** client jar merged for Fabric (`26.2-0.19.5.jar`, as the Fabric launcher/installer
  writes into `versions/`), with its `.json`;
- **Fabric Loader 0.19.5** and the libraries listed in that json (`net/fabricmc/...`, `org/ow2/asm/...`,
  `net/fabricmc/sponge-mixin`, `io/github/llamalad7/mixinextras`, `com/google/...`, `commons-*`...), laid out as in
  the launcher's `libraries/` folder;
- **Fabric API 0.160.0+26.2** (`fabric-api-0.160.0+26.2.jar`).

The simplest way to collect them: install Fabric 0.19.5 for 26.2 with the official
[Fabric installer](https://fabricmc.net/use/installer/) into a launcher folder, launch once, then
`tar -cf mcdeps.tar -C <launcher> versions/26.2-0.19.5 libraries` (flattened so the version jar sits at the top
level) and add the Fabric API jar from [Modrinth](https://modrinth.com/mod/fabric-api).

Then, once:

```
mod/setup-deps.sh /path/to/mcdeps.tar
```

It unpacks to `mod/deps/` and writes compile-only copies to `mod/cdeps/` (class files lowered to Java 21 by
`patchver.py` so `javac --release 21` can read them).

**Optional compile-only jars** (not included, never shipped):

- Distant Horizons API: put `dh-api-compileonly.jar` (from the
  [Distant Horizons API](https://gitlab.com/distant-horizons-team/distant-horizons) release for 26.2, LGPL-3.0) in
  `mod/lib/`. Needed for the engine's DH support.
- Controlify (for TATV's controller input): copy the Controlify jar for 26.2 through `mod/patchver.py` into
  `mods/tatv/lib/controlify-compileonly.jar`.

## 2. Build

```
mods/build.sh all                 # every module; jars land in mods/<module>/out/
mods/build.sh earthdata           # or one module (build the modules it uses first)
```

Modules: `core` (the engine library), `earthdata`, `earthish`, `fossil-core`, `earth-fossils`, `earthish-fossils`,
`landmarks-core`, `earth-landmarks`, `earthish-landmarks`, `tatv`. Each has a `module.json` naming the modules it
compiles against (`uses`) and the libraries nested in its jar (`nest`, Fabric jar-in-jar).

**Earth Data's base tiles:** the coarse planet in `mods/earthdata/src/main/resources/worldbound/base/` isn't in this
folder (46 MB). Build it with `tools/build_base.py` from GEBCO and Köppen-Geiger (see the script's header), or copy
`worldbound/base/` out of a released `earthdata` jar.

## 3. Test

```
mods/tests/test.sh                # CoreTests: ~10,800 checks (needs Python 3 with numpy for one tile test)
JOPTS=-Dworldbound.debug=true SCN=t MODS="earthdata earth-fossils" mods/runserver.sh "earth where" "earth fauna dinotest"
```

`runserver.sh` boots a headless dedicated server with the chosen modules (Java 25 from `JAVA25=...` or
`pip install jdk4py`) and runs console commands; the log is `mods/run/<SCN>/server.log`.
`fossil-core/art.sh` repaints the animals' textures and writes their models, loot tables, lang and sounds.

## 4. Data tools

`tools/` is the Python pipeline that turns public geodata into the tile packs the mod reads
(`pip install -r tools/requirements.txt`). Each script's header says where its source data comes from.
