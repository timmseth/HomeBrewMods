# Data attribution and third-party notices

WorldBound's code is MIT-licensed (see `LICENSE`). The geodata it ships, streams or builds is derived from
the public datasets below and stays under their licences. Keep this file with any copy of the jar or of a
data pack you build.

## Shipped in the jar

The coarse (~5 km) planet in `worldbound/base/` and the place index `worldbound/places.tsv.gz`.

- **GEBCO Grid** (elevation and bathymetry). GEBCO Compilation Group (2026), GEBCO 2026 Grid.
  https://www.gebco.net/ — free to use, copy and redistribute with attribution. GEBCO, IHO and IOC do not
  endorse WorldBound.
- **Köppen-Geiger climate classification**, 1991–2020, 1 km. Beck, H.E. et al. (2023), "High-resolution
  (1 km) Köppen-Geiger maps for 1901–2099 based on constrained CMIP6 projections", *Scientific Data* 10, 724.
  https://www.gloh2o.org/koppen/ — CC BY 4.0.
- **GeoNames** (countries, states and cities, via the `geonamescache` Python package).
  https://www.geonames.org/ — CC BY 4.0.
- **US ZIP code centroids** via the `zipcodes` Python package (Sean Pianka, MIT).
- **Landmarks** (`landmarks.tsv`): coordinates compiled by hand from public sources.

## Streamed at run time (when `streaming.enabled` is on)

- **Copernicus DEM GLO-30** (public instance), read from the AWS Open Data bucket `copernicus-dem-30m`.
  Produced using Copernicus WorldDEM-30 © DLR e.V. 2010–2014 and © Airbus Defence and Space GmbH 2014–2018,
  provided under COPERNICUS by the European Union and ESA; all rights reserved. The organisations in charge of
  the Copernicus programme by law or by delegation do not incur any liability for any use of the Copernicus
  WorldDEM-30.
- **ESA WorldCover 10 m 2021 v200**, read from the AWS bucket `esa-worldcover`. © ESA WorldCover project
  2021 / Contains modified Copernicus Sentinel data (2021) processed by the ESA WorldCover consortium.
  Zanaga, D. et al. (2022), doi:10.5281/zenodo.7254221 — CC BY 4.0.

## Optional data packs (built by you with `tools/`, not distributed)

- **Copernicus DEM GLO-90.** Produced using Copernicus WorldDEM-90 © DLR e.V. 2010–2014 and © Airbus Defence
  and Space GmbH 2014–2018, provided under COPERNICUS by the European Union and ESA; all rights reserved.
- **Copernicus Global Land Service Land Cover 100 m (CGLS-LC100) Collection 3, 2019.** Buchhorn, M. et al.
  (2020), doi:10.5281/zenodo.3939050. © European Union, Copernicus Land Monitoring Service — free use with
  attribution.
- **HydroRIVERS v1.0.** Lehner, B., Grill, G. (2013), *Hydrological Processes* 27(15): 2171–2186.
  https://www.hydrosheds.org — free for commercial and non-commercial use with attribution.
- **HydroLAKES v1.0.** Messager, M.L. et al. (2016), *Nature Communications* 7: 13603.
  https://www.hydrosheds.org — CC BY 4.0.

## Wildlife facts

Dinosaur sizes and fossil-site locations are published scientific estimates and descriptions (formations
and localities as reported in the literature, with many body sizes after G. S. Paul, *The Princeton Field
Guide to Dinosaurs*). They are facts, compiled by hand; no database is redistributed. Colours are guesses.

## Other mods

- **Terralith** and **Terrestria**: WorldBound refers to their biome IDs when they are installed. It does not
  include any of their files. Terralith's licence does not allow its code to be redistributed inside another
  mod, so the Terralith ground-block compatibility pack is not shipped; `tools/terralith_compat.py` builds it
  from your own copy of Terralith, for your own use.
- **Distant Horizons**: WorldBound compiles against the Distant Horizons API (LGPL-3.0) and calls it only
  when Distant Horizons is installed. No Distant Horizons code is included.

## Minecraft

Minecraft is © Mojang Studios / Microsoft. WorldBound is not an official Minecraft product and is not
approved by or associated with Mojang or Microsoft. It contains no Mojang assets.
