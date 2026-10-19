package com.powertrade;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Nível de cada habilidade por jogador, salvo junto com o mundo (chaveado por UUID, então sobrevive à morte). */
public class PowerData extends SavedData {
    private static final Codec<Map<String, Map<String, Integer>>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.INT));

    public static final Codec<PowerData> CODEC = MAP_CODEC.xmap(PowerData::new, d -> d.data);

    public static final SavedDataType<PowerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PowerTradeMod.MOD_ID, "levels"),
            PowerData::new,
            CODEC);

    private final Map<String, Map<String, Integer>> data = new HashMap<>();

    public PowerData() {
    }

    private PowerData(Map<String, Map<String, Integer>> loaded) {
        for (Map.Entry<String, Map<String, Integer>> e : loaded.entrySet()) {
            data.put(e.getKey(), new HashMap<>(e.getValue()));
        }
    }

    public static PowerData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public int getLevel(UUID player, Skill skill) {
        Map<String, Integer> m = data.get(player.toString());
        return m == null ? 0 : m.getOrDefault(skill.id(), 0);
    }

    public void setLevel(UUID player, Skill skill, int level) {
        data.computeIfAbsent(player.toString(), k -> new HashMap<>()).put(skill.id(), level);
        setDirty();
    }
}
