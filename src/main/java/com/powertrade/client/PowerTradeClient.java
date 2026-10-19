package com.powertrade.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.powertrade.PowerTradeMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Ponto de entrada só do cliente: tecla configurável (K) e botão "Poder" no inventário. */
@Mod(value = PowerTradeMod.MOD_ID, dist = Dist.CLIENT)
public class PowerTradeClient {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PowerTradeMod.MOD_ID, "main"));

    /** Tecla padrão K (código GLFW 75); o jogador troca em Opções > Controles. */
    public static final KeyMapping OPEN_KEY =
            new KeyMapping("key.powertrade.open", InputConstants.Type.KEYSYM, 75, CATEGORY);

    public PowerTradeClient(IEventBus modBus) {
        modBus.addListener(PowerTradeClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(PowerTradeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(PowerTradeClient::onScreenInit);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new PowerScreen());
            }
        }
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            int x = (screen.width - 176) / 2 + 176 + 6;
            int y = (screen.height - 166) / 2 + 6;
            Button button = Button.builder(Component.literal("Poder"),
                            b -> Minecraft.getInstance().setScreen(new PowerScreen()))
                    .bounds(x, y, 56, 18)
                    .build();
            event.addListener(button);
        }
    }
}
