#!/usr/bin/env python3
"""100 notable places for the Earth picker's "Landmarks" list.

Writes mod/src/main/resources/worldbound/landmarks.tsv:  name \\t category \\t lat \\t lon \\t zoom_deg_span
zoom_deg_span is how many degrees of longitude the view should show when jumping there.
Coordinates are the feature's best viewpoint, not an administrative centroid.
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "mod", "src", "main", "resources", "worldbound", "landmarks.tsv")

L = [
    # name, category, lat, lon, span
    # --- Seth's own views (screenshot tour + obelisks) ---
    ("Ruhr valley at Mülheim", "river", 51.4190, 6.8740, 0.25),
    ("Tafí del Valle, Tucumán", "valley", -26.8241, -65.2226, 0.5),
    # --- mountains ---
    ("Mount Everest", "mountain", 27.988, 86.925, 0.6),
    ("K2", "mountain", 35.881, 76.513, 0.6),
    ("Kangchenjunga", "mountain", 27.703, 88.147, 0.6),
    ("Annapurna", "mountain", 28.596, 83.820, 0.8),
    ("Mont Blanc", "mountain", 45.833, 6.865, 0.6),
    ("Matterhorn", "mountain", 45.976, 7.658, 0.4),
    ("Denali", "mountain", 63.069, -151.007, 1.0),
    ("Mount Rainier", "mountain", 46.852, -121.760, 0.6),
    ("Mount Shasta", "mountain", 41.409, -122.195, 0.6),
    ("Mount Fuji", "mountain", 35.361, 138.727, 0.6),
    ("Kilimanjaro", "mountain", -3.066, 37.353, 0.8),
    ("Aconcagua", "mountain", -32.653, -70.011, 0.8),
    ("Mount Elbrus", "mountain", 43.355, 42.439, 0.6),
    ("Mount Kosciuszko", "mountain", -36.456, 148.263, 0.6),
    ("Mount Cook / Aoraki", "mountain", -43.595, 170.142, 0.6),
    ("Mount Whitney", "mountain", 36.579, -118.292, 0.6),
    ("Borah Peak", "mountain", 44.137, -113.781, 0.5),
    ("Grand Teton", "mountain", 43.741, -110.802, 0.5),
    ("Mount Olympus (Greece)", "mountain", 40.086, 22.358, 0.5),
    ("Mount Ararat", "mountain", 39.702, 44.298, 0.8),
    ("Mauna Kea", "mountain", 19.821, -155.468, 1.2),
    ("Mount Etna", "volcano", 37.751, 14.993, 0.6),
    ("Vesuvius", "volcano", 40.822, 14.428, 0.4),
    ("Krakatoa", "volcano", -6.102, 105.423, 0.6),
    ("Mount St. Helens", "volcano", 46.191, -122.195, 0.4),
    ("Yellowstone Caldera", "volcano", 44.428, -110.588, 1.2),
    ("Kīlauea", "volcano", 19.421, -155.287, 0.6),
    # --- canyons, valleys, deserts ---
    ("Grand Canyon (South Rim)", "canyon", 36.104, -112.113, 1.2),
    ("Hells Canyon", "canyon", 45.350, -116.500, 0.8),
    ("Yosemite Valley", "canyon", 37.746, -119.593, 0.4),
    ("Zion Canyon", "canyon", 37.270, -112.948, 0.4),
    ("Bryce Canyon", "canyon", 37.593, -112.187, 0.4),
    ("Copper Canyon (Mexico)", "canyon", 27.500, -107.800, 1.5),
    ("Fish River Canyon", "canyon", -27.580, 17.590, 0.8),
    ("Colca Canyon", "canyon", -15.610, -71.970, 0.8),
    ("Yarlung Tsangpo Gorge", "canyon", 29.800, 95.000, 1.5),
    ("Death Valley (Badwater)", "desert", 36.230, -116.767, 1.0),
    ("Sahara (Erg Chebbi)", "desert", 31.150, -3.980, 1.5),
    ("Rub' al Khali", "desert", 20.000, 50.000, 6.0),
    ("Gobi Desert", "desert", 42.800, 105.000, 8.0),
    ("Atacama Desert", "desert", -24.500, -69.250, 3.0),
    ("Namib Sand Sea", "desert", -24.700, 15.300, 1.5),
    ("Great Sand Dunes", "desert", 37.750, -105.520, 0.4),
    ("Uluru", "desert", -25.345, 131.036, 0.5),
    ("Monument Valley", "desert", 36.998, -110.098, 0.6),
    ("Salar de Uyuni", "desert", -20.133, -67.489, 1.5),
    ("Bonneville Salt Flats", "desert", 40.750, -113.900, 0.8),
    # --- water & coasts ---
    ("Niagara Falls", "water", 43.080, -79.075, 0.3),
    ("Victoria Falls", "water", -17.924, 25.857, 0.4),
    ("Iguazu Falls", "water", -25.695, -54.437, 0.4),
    ("Angel Falls", "water", 5.968, -62.536, 0.5),
    ("Lake Baikal", "water", 53.500, 108.000, 6.0),
    ("Great Salt Lake", "water", 41.100, -112.500, 1.5),
    ("Lake Tahoe", "water", 39.100, -120.030, 0.6),
    ("Crater Lake", "water", 42.944, -122.109, 0.4),
    ("Dead Sea", "water", 31.500, 35.480, 1.0),
    ("Caspian Sea", "water", 42.000, 50.500, 12.0),
    ("Strait of Gibraltar", "coast", 35.950, -5.600, 1.2),
    ("Bering Strait", "coast", 65.800, -169.000, 3.0),
    ("Cape of Good Hope", "coast", -34.357, 18.474, 0.6),
    ("Cape Horn", "coast", -55.980, -67.270, 1.0),
    ("Norwegian Fjords (Geiranger)", "coast", 62.100, 7.100, 1.0),
    ("Milford Sound", "coast", -44.670, 167.925, 0.6),
    ("Great Barrier Reef", "coast", -18.300, 147.700, 3.0),
    ("Ha Long Bay", "coast", 20.910, 107.180, 0.6),
    ("Amalfi Coast", "coast", 40.633, 14.600, 0.5),
    ("White Cliffs of Dover", "coast", 51.133, 1.362, 0.4),
    ("Big Sur", "coast", 36.270, -121.810, 0.8),
    ("Mariana Trench (Challenger Deep)", "ocean", 11.373, 142.592, 2.0),
    ("Mid-Atlantic Ridge (Azores)", "ocean", 38.500, -28.000, 4.0),
    ("Galápagos Islands", "island", -0.800, -90.500, 3.0),
    ("Iceland (Vatnajökull)", "island", 64.400, -16.800, 4.0),
    ("Svalbard", "island", 78.500, 16.000, 8.0),
    ("Bora Bora", "island", -16.500, -151.740, 0.4),
    ("Santorini", "island", 36.400, 25.430, 0.4),
    ("Easter Island", "island", -27.112, -109.350, 0.6),
    # --- ice & polar ---
    ("South Pole", "polar", -89.900, 0.000, 20.0),
    ("Antarctic Peninsula", "polar", -68.000, -65.000, 10.0),
    ("Greenland Ice Sheet (Summit)", "polar", 72.580, -38.460, 8.0),
    ("Patagonian Ice Field (Perito Moreno)", "polar", -50.480, -73.050, 1.5),
    ("Franz Josef Glacier", "polar", -43.470, 170.180, 0.5),
    # --- plateaus, plains, big landforms ---
    ("Tibetan Plateau (edge)", "plateau", 28.600, 85.000, 6.0),
    ("Altiplano (La Paz)", "plateau", -16.500, -68.150, 2.0),
    ("Ethiopian Highlands (Simien)", "plateau", 13.200, 38.100, 2.0),
    ("Colorado Plateau (Moab)", "plateau", 38.573, -109.550, 1.5),
    ("Snake River Plain", "plain", 43.000, -113.500, 3.0),
    ("Great Plains (Nebraska)", "plain", 41.500, -100.000, 6.0),
    ("Amazon Basin (Manaus)", "plain", -3.100, -60.000, 8.0),
    ("Serengeti", "plain", -2.333, 34.833, 2.0),
    ("Okavango Delta", "plain", -19.300, 22.900, 2.0),
    ("Ganges Delta", "plain", 22.500, 89.500, 3.0),
    ("Nile Delta", "plain", 31.000, 31.000, 2.0),
    ("Great Rift Valley (Lake Turkana)", "rift", 3.500, 36.000, 4.0),
    ("Baikal Rift / Lake Baikal", "rift", 53.000, 107.000, 6.0),
    # --- cities (the terrain under them) ---
    ("Pocatello, Idaho", "city", 42.871, -112.445, 0.5),
    ("Boise, Idaho", "city", 43.615, -116.202, 0.6),
    ("Salt Lake City", "city", 40.761, -111.891, 0.8),
    ("New York City", "city", 40.713, -74.006, 0.8),
    ("San Francisco Bay", "city", 37.775, -122.419, 1.0),
    ("Rio de Janeiro", "city", -22.907, -43.173, 0.6),
    ("Cape Town (Table Mountain)", "city", -33.963, 18.410, 0.5),
    ("Hong Kong", "city", 22.320, 114.170, 0.5),
    ("Tokyo Bay", "city", 35.600, 139.800, 1.2),
    ("Istanbul (Bosphorus)", "city", 41.100, 29.050, 0.6),
    ("Kathmandu Valley", "city", 27.700, 85.330, 0.6),
    ("Mexico City (Valley of Mexico)", "city", 19.430, -99.130, 1.0),
]


def main():
    assert len(L) >= 100, len(L)
    names = [x[0] for x in L]
    assert len(set(names)) == len(names), "duplicate landmark"
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        for name, cat, lat, lon, span in L:
            f.write(f"{name}\t{cat}\t{lat:.4f}\t{lon:.4f}\t{span}\n")
    print(len(L), "landmarks ->", OUT)


if __name__ == "__main__":
    main()
