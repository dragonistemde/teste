package com.powertrade;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(PowerTradeMod.MOD_ID)
public class PowerTradeMod {
    public static final String MOD_ID = "powertrade";

    public PowerTradeMod(IEventBus modBus) {
        modBus.addListener(PowerTradeMod::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.CODEC,
                (payload, ctx) -> ClientPowerState.set(payload.levels()));
        registrar.playToServer(BuyPayload.TYPE, BuyPayload.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) {
                PowerLogic.buy(player, payload.skill());
            }
        });
    }
}
