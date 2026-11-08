package com.seth.fossilrecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.seth.fossilrecord.dino.DinoSpecies;
import com.seth.fossilrecord.dino.Dinos;
import com.seth.fossilrecord.dino.Roster;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dev tool: writes the dinosaurs' resource files from the roster - item model definitions and models for the spawn
 * eggs, loot tables, and their entries in en_us.json and sounds.json (other entries are kept as they are).
 * {@code DinoGen <src/main/resources dir>}
 */
public final class DinoGen {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void main(String[] a) throws IOException {
        File res = new File(a[0]);
        File assets = new File(res, "assets/fossilrecord"), data = new File(res, "data/fossilrecord");
        new File(assets, "items").mkdirs();
        new File(assets, "models/item").mkdirs();
        new File(data, "loot_table/entities").mkdirs();

        // ---- language: keep everything that isn't a dinosaur's, then add theirs
        File langF = new File(assets, "lang/en_us.json");
        Map<String, String> lang = new LinkedHashMap<>();
        JsonObject old = JsonParser.parseString(Files.readString(langF.toPath())).getAsJsonObject();
        for (var e : old.entrySet()) if (!isDinoKey(e.getKey())) lang.put(e.getKey(), e.getValue().getAsString());
        lang.put("itemGroup.fossilrecord.animals", "Fossil Record");

        File soundF = new File(assets, "sounds.json");
        JsonObject sounds = JsonParser.parseString(Files.readString(soundF.toPath())).getAsJsonObject();
        JsonObject newSounds = new JsonObject();
        for (var e : sounds.entrySet()) if (!isDinoSound(e.getKey())) newSounds.add(e.getKey(), e.getValue());

        for (DinoSpecies s : Roster.ALL) {
            String egg = s.id + "_spawn_egg";
            JsonObject item = new JsonObject(), model = new JsonObject();
            model.addProperty("type", "minecraft:model");
            model.addProperty("model", "fossilrecord:item/" + egg);
            item.add("model", model);
            write(new File(assets, "items/" + egg + ".json"), item);
            JsonObject m = new JsonObject(), tex = new JsonObject();
            m.addProperty("parent", "minecraft:item/generated");
            tex.addProperty("layer0", "fossilrecord:item/" + egg);
            m.add("textures", tex);
            write(new File(assets, "models/item/" + egg + ".json"), m);
            write(new File(data, "loot_table/entities/" + s.id + ".json"), loot(s));

            String n = s.shortName;
            lang.put("entity.fossilrecord." + s.id, n);
            lang.put("item.fossilrecord." + egg, n + " Spawn Egg");
            DinoSpecies.Voice v = s.voice;
            String hit = switch (s.attack) {
                case BITE -> "bites"; case GORE -> "charges"; case TAIL -> "swings its tail"; case HEADBUTT -> "head-butts";
                case CLAW -> "slashes"; case KICK -> "kicks"; case STOMP -> "stamps";
            };
            lang.put(sub(s, "ambient"), n + " " + v.verb());
            lang.put(sub(s, "hurt"), n + " hurts");
            lang.put(sub(s, "death"), n + " dies");
            lang.put(sub(s, "attack"), n + " " + hit);
            lang.put(sub(s, "roar"), n + " roars");
            newSounds.add("entity." + s.id + ".ambient", sound(v.ambient(), v.ambientPitch(), sub(s, "ambient")));
            newSounds.add("entity." + s.id + ".hurt", sound(v.hurt(), v.hurtPitch(), sub(s, "hurt")));
            newSounds.add("entity." + s.id + ".death", sound(v.death(), v.deathPitch(), sub(s, "death")));
            newSounds.add("entity." + s.id + ".attack", sound(v.attack(), v.attackPitch(), sub(s, "attack")));
            newSounds.add("entity." + s.id + ".roar", sound(v.roar(), v.roarPitch(), sub(s, "roar")));
        }
        JsonObject langOut = new JsonObject();
        for (var e : lang.entrySet()) langOut.addProperty(e.getKey(), e.getValue());
        write(langF, langOut);
        // the laid egg: one item, its look chosen by the species id in its custom model data
        JsonObject sel = new JsonObject(), eggDef = new JsonObject(), fb = new JsonObject();
        com.google.gson.JsonArray cases = new com.google.gson.JsonArray();
        sel.addProperty("type", "minecraft:select");
        sel.addProperty("property", "minecraft:custom_model_data");
        sel.addProperty("index", 0);
        for (DinoSpecies s : Roster.ALL) {
            JsonObject c = new JsonObject(), m = new JsonObject();
            m.addProperty("type", "minecraft:model");
            m.addProperty("model", "fossilrecord:item/" + s.id + "_spawn_egg");
            c.addProperty("when", s.id);
            c.add("model", m);
            cases.add(c);
        }
        sel.add("cases", cases);
        fb.addProperty("type", "minecraft:model");
        fb.addProperty("model", "fossilrecord:item/velociraptor_spawn_egg");
        sel.add("fallback", fb);
        eggDef.add("model", sel);
        write(new File(assets, "items/dino_egg.json"), eggDef);
        write(soundF, newSounds);
        System.out.println("wrote resources for " + Roster.ALL.size() + " dinosaurs (" + Dinos.SOUND_KINDS.length * Roster.ALL.size() + " sounds)");
    }

    static String sub(DinoSpecies s, String kind) { return "subtitles.fossilrecord.entity." + s.id + "." + kind; }

    static boolean isDinoKey(String k) {
        for (DinoSpecies s : Roster.ALL)
            if (k.equals("entity.fossilrecord." + s.id) || k.equals("item.fossilrecord." + s.id + "_spawn_egg") || k.startsWith("subtitles.fossilrecord.entity." + s.id + ".")) return true;
        return false;
    }

    static boolean isDinoSound(String k) {
        for (DinoSpecies s : Roster.ALL) if (k.startsWith("entity." + s.id + ".")) return true;
        return false;
    }

    static JsonObject sound(String event, float pitch, String subtitle) {
        JsonObject o = new JsonObject(), snd = new JsonObject();
        JsonArray list = new JsonArray();
        snd.addProperty("name", event.contains(":") ? event : "minecraft:" + event);
        snd.addProperty("type", "event");
        snd.addProperty("pitch", Math.max(0.5f, Math.min(2f, pitch)));
        list.add(snd);
        o.add("sounds", list);
        o.addProperty("subtitle", subtitle);
        return o;
    }

    /** Meat (beef from the big ones, chicken from the small and feathered), bones, and hide or feathers, all by weight. */
    static JsonObject loot(DinoSpecies s) {
        JsonObject t = new JsonObject();
        t.addProperty("type", "minecraft:entity");
        JsonArray pools = new JsonArray();
        double m = s.mass;
        boolean feathered = s.body.feathers;
        int meat = clamp((int) Math.round(Math.log(m / 15) / Math.log(2)), 1, 14);
        pools.add(pool(feathered || m < 60 ? "minecraft:chicken" : "minecraft:beef", m < 10 ? 0 : Math.max(1, meat / 2), meat, true));
        int bones = clamp((int) Math.round(Math.log(m / 8) / Math.log(2)), 1, 18);
        pools.add(pool("minecraft:bone", Math.max(0, bones / 2), bones, false));
        if (feathered) pools.add(pool("minecraft:feather", 1, clamp(2 + (int) Math.log10(m), 2, 6), false));
        else if (m >= 200) {
            int hide = clamp((int) Math.round(Math.log(m / 100) / Math.log(2)), 1, 10);
            pools.add(pool("minecraft:leather", Math.max(0, hide / 2), hide, false));
        }
        t.add("pools", pools);
        t.addProperty("random_sequence", "fossilrecord:entities/" + s.id);
        return t;
    }

    static JsonObject pool(String item, int min, int max, boolean smelts) {
        JsonObject pool = new JsonObject(), entry = new JsonObject();
        pool.addProperty("rolls", 1.0);
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", item);
        JsonArray fns = new JsonArray();
        fns.add(countFn("minecraft:set_count", min, max, null));
        if (smelts) fns.add(JsonParser.parseString("""
                {"function": "minecraft:furnace_smelt", "conditions": [{"condition": "minecraft:any_of", "terms": [
                  {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:flags": {"is_on_fire": true}}},
                  {"condition": "minecraft:entity_properties", "entity": "direct_attacker", "predicate": {"minecraft:equipment": {"mainhand":
                    {"predicates": {"minecraft:enchantments": [{"enchantments": "#minecraft:smelts_loot"}]}}}}}]}]}""").getAsJsonObject());
        fns.add(countFn("minecraft:enchanted_count_increase", 0, 1, "minecraft:looting"));
        entry.add("functions", fns);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        return pool;
    }

    static JsonObject countFn(String fn, int min, int max, String enchantment) {
        JsonObject f = new JsonObject(), c = new JsonObject();
        f.addProperty("function", fn);
        c.addProperty("type", "minecraft:uniform");
        c.addProperty("min", (double) min);
        c.addProperty("max", (double) Math.max(min, max));
        f.add("count", c);
        if (enchantment != null) f.addProperty("enchantment", enchantment);
        return f;
    }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static void write(File f, JsonObject o) throws IOException {
        Files.writeString(f.toPath(), GSON.toJson(o) + "\n", StandardCharsets.UTF_8);
    }
}
