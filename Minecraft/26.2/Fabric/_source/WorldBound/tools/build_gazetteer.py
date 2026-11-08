#!/usr/bin/env python3
"""Build the place index the Earth picker searches: countries, US states, cities (>15k pop), US zips.

Sources are two pip packages that bundle GeoNames / USPS-derived data (no network at run time):
    pip install geonamescache zipcodes
Output: mod/src/main/resources/worldbound/places.tsv.gz with lines
    type \\t name \\t region \\t country \\t lat \\t lon \\t population
type is one of country, state, city, zip. Country/state coordinates are population-weighted
centroids of their cities (countries use their capital when it is in the city list).
"""
import gzip
import os
import sys
from collections import defaultdict

import geonamescache
import zipcodes

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "mod", "src", "main", "resources", "worldbound", "places.tsv.gz")


def main():
    gc = geonamescache.GeonamesCache()
    cities = gc.get_cities()
    countries = gc.get_countries()
    states = gc.get_us_states()
    rows = []
    by_country = defaultdict(list)
    by_state = defaultdict(list)
    for c in cities.values():
        rows.append(("city", c["name"], c["admin1code"] if c["countrycode"] == "US" else "", c["countrycode"],
                     c["latitude"], c["longitude"], c["population"]))
        by_country[c["countrycode"]].append(c)
        if c["countrycode"] == "US":
            by_state[c["admin1code"]].append(c)

    def centroid(cs):
        w = sum(max(c["population"], 1) for c in cs)
        return (sum(c["latitude"] * max(c["population"], 1) for c in cs) / w,
                sum(c["longitude"] * max(c["population"], 1) for c in cs) / w)

    for iso, co in countries.items():
        cs = by_country.get(iso, [])
        cap = [c for c in cs if c["name"] == co["capital"]]
        if cap:
            lat, lon = cap[0]["latitude"], cap[0]["longitude"]
        elif cs:
            lat, lon = centroid(cs)
        else:
            continue
        rows.append(("country", co["name"], co["continentcode"], iso, lat, lon, co["population"]))
    for code, st in states.items():
        cs = by_state.get(code, [])
        if not cs:
            continue
        lat, lon = centroid(cs)
        rows.append(("state", st["name"], code, "US", lat, lon, sum(c["population"] for c in cs)))
    for z in zipcodes.list_all():
        if z.get("lat") and z.get("long") and z.get("active"):
            rows.append(("zip", z["zip_code"], f'{z["city"]}, {z["state"]}', "US", float(z["lat"]), float(z["long"]), 0))

    order = {"country": 0, "state": 1, "city": 2, "zip": 3}
    rows.sort(key=lambda r: (order[r[0]], -int(r[6] or 0), r[1]))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with gzip.open(OUT, "wt", encoding="utf-8") as f:
        for r in rows:
            f.write("\t".join([r[0], r[1].replace("\t", " "), str(r[2]), str(r[3]), f"{r[4]:.4f}", f"{r[5]:.4f}", str(int(r[6] or 0))]) + "\n")
    counts = defaultdict(int)
    for r in rows:
        counts[r[0]] += 1
    print(dict(counts), f"-> {OUT} ({os.path.getsize(OUT) // 1024} KB)")


if __name__ == "__main__":
    main()
