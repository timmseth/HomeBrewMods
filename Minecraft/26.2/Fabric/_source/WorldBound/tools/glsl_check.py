"""Compile and link WorldBound's core shaders in headless Chromium (WebGL2) - a syntax and interface check.

Minecraft 26.2 compiles GLSL 330 core; WebGL2 takes GLSL ES 3.00, which is close enough to catch typos, type
errors, undeclared names and vertex/fragment interface mismatches. Vanilla #moj_import includes are inlined from
the game jar. It does not prove the shaders behave right on the GPU - only Seth's game does that.

    python3 tools/glsl_check.py [path/to/mc-26.2.jar]
"""
import json
import pathlib
import re
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
JAR = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "mod" / "cdeps" / "mc-26.2.jar"
OURS = ROOT / "mod" / "src" / "main" / "resources" / "assets" / "worldbound" / "shaders" / "core"

jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return jar.read(path).decode()


def es(src, seen=None):
    """GLSL 330 core -> ES 3.00: version line, precision, inlined includes."""
    seen = set() if seen is None else seen

    def inline(m):
        ns, name = m.group(1), m.group(2)
        if (ns, name) in seen:
            return ""
        seen.add((ns, name))
        body = vanilla(f"assets/{ns}/shaders/include/{name}")
        return re.sub(r"^#version.*$", "", es_body(body, seen), flags=re.M)

    return re.sub(r'#moj_import\s*<([a-z_]+):([^>]+)>', inline, src)


def es_body(src, seen):
    return es(src, seen)


def program(vsh, fsh):
    head = "#version 300 es\nprecision highp float;\nprecision highp int;\nprecision highp sampler2D;\n"
    strip = lambda s: re.sub(r"^#version.*$", "", s, flags=re.M)
    return head + strip(es(vsh)), head + strip(es(fsh))


PAIRS = {
    "horizon": (OURS.joinpath("horizon.vsh").read_text(), OURS.joinpath("horizon.fsh").read_text()),
    "horizon_blit": (vanilla("assets/minecraft/shaders/core/screenquad.vsh"), OURS.joinpath("horizon_blit.fsh").read_text()),
}

PAGE = """<!doctype html><canvas id=c></canvas><script>
const gl = document.getElementById('c').getContext('webgl2');
window.check = (vs, fs) => {
  const out = {};
  const mk = (type, src, key) => { const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s);
    out[key] = gl.getShaderParameter(s, gl.COMPILE_STATUS) ? 'ok' : gl.getShaderInfoLog(s); return s; };
  const v = mk(gl.VERTEX_SHADER, vs, 'vertex'), f = mk(gl.FRAGMENT_SHADER, fs, 'fragment');
  const p = gl.createProgram(); gl.attachShader(p, v); gl.attachShader(p, f); gl.linkProgram(p);
  out.link = gl.getProgramParameter(p, gl.LINK_STATUS) ? 'ok' : gl.getProgramInfoLog(p);
  if (out.link === 'ok') {
    out.uniformBlocks = []; const nb = gl.getProgramParameter(p, gl.ACTIVE_UNIFORM_BLOCKS);
    for (let i = 0; i < nb; i++) out.uniformBlocks.push(gl.getActiveUniformBlockName(p, i) + ':' + gl.getActiveUniformBlockParameter(p, i, gl.UNIFORM_BLOCK_DATA_SIZE));
    out.samplers = []; const nu = gl.getProgramParameter(p, gl.ACTIVE_UNIFORMS);
    for (let i = 0; i < nu; i++) { const u = gl.getActiveUniform(p, i); if (u.type === gl.SAMPLER_2D) out.samplers.push(u.name); }
  }
  return out;
};
</script>"""

if __name__ == "__main__":
    from playwright.sync_api import sync_playwright
    bad = 0
    with sync_playwright() as pw:
        b = pw.chromium.launch(args=["--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader"])
        page = b.new_page()
        page.set_content(PAGE)
        for name, (v, f) in PAIRS.items():
            vs, fs = program(v, f)
            r = page.evaluate("([v, f]) => check(v, f)", [vs, fs])
            ok = r["vertex"] == "ok" and r["fragment"] == "ok" and r["link"] == "ok"
            bad += not ok
            print(f"{name}: {'OK' if ok else 'FAILED'} {json.dumps(r)}")
        b.close()
    sys.exit(1 if bad else 0)
