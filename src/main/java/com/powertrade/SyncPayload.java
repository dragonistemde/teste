package com.powertrade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Servidor → cliente: níveis atuais do jogador. */
public record SyncPayload(Map<String, Integer> levels) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PowerTradeMod.MOD_ID, "sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC =
            CustomPacketPayload.codec(SyncPayload::write, SyncPayload::new);

    public SyncPayload(RegistryFriendlyByteBuf buf) {
        this(read(buf));
    }

    private static Map<String, Integer> read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            int value = buf.readVarInt();
            map.put(key, value);
        }
        return map;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(levels.size());
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue());
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
