"""Reads BIOME_TABLE.md so the markdown stays the single source of truth for the tools."""
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
TABLE_MD = os.path.join(HERE, "..", "BIOME_TABLE.md")

# WorldCover legend colours (ESA), for cover previews.
COVER_COLORS = {
    0: (0, 0, 0), 10: (0, 100, 0), 20: (255, 187, 34), 30: (255, 255, 76), 40: (240, 150, 255),
    50: (250, 0, 0), 60: (180, 180, 180), 70: (240, 240, 240), 80: (0, 100, 200),
    90: (0, 150, 160), 95: (0, 207, 117), 100: (250, 230, 160),
}


def load():
    """Returns {id: {"code", "name", "default", "tree", "bare", "snowline", "rgb"}}."""
    rows = {}
    with open(TABLE_MD, encoding="utf-8") as f:
        for line in f:
            m = re.match(r"^\|\s*(\d+)\s*\|\s*([A-Za-z]+)\s*\|(.*)$", line)
            if not m:
                continue
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            if len(cells) < 8:
                continue
            cid = int(cells[0])
            rgb = tuple(int(x) for x in cells[7].split(","))

            def biome(c):
                return re.sub(r"`|\s*\(.*\)", "", c).strip()

            rows[cid] = {
                "code": cells[1], "name": cells[2], "default": biome(cells[3]),
                "tree": biome(cells[4]), "bare": biome(cells[5]),
                "snowline": int(cells[6]), "rgb": rgb,
            }
    if len(rows) != 30:
        raise SystemExit(f"BIOME_TABLE.md: expected 30 classes, parsed {len(rows)}")
    return rows


if __name__ == "__main__":
    for k, v in sorted(load().items()):
        print(k, v)
