package com.powertrade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente → servidor: pedido para comprar o próximo nível de uma habilidade. */
public record BuyPayload(String skill) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BuyPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PowerTradeMod.MOD_ID, "buy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuyPayload> CODEC =
            CustomPacketPayload.codec(BuyPayload::write, BuyPayload::new);

    public BuyPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(skill);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
