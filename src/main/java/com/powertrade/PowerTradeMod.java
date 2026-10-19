package com.powertrade;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(PowerTradeMod.MOD_ID)
public class PowerTradeMod {
    public static final String MOD_ID = "powertrade";

    public PowerTradeMod(IEventBus modBus) {
        // Tudo é registrado via @EventBusSubscriber em PowerEvents.
    }
}
