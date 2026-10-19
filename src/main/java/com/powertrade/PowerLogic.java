package com.powertrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Lógica do servidor: compra, aplicação de atributos e sincronização com o cliente. */
public final class PowerLogic {
    private PowerLogic() {}

    private static final int MAIN_SLOTS = 36;

    public static MinecraftServer server(ServerPlayer player) {
        return player.level().getServer();
    }

    /** Tenta comprar o próximo nível. Retorna true se comprou. */
    public static boolean buy(ServerPlayer player, String skillId) {
        MinecraftServer server = server(player);
        if (server == null) return false;

        Skill skill = Skill.byId(skillId);
        if (skill == null) {
            fail(player, "Habilidade desconhecida: " + skillId);
            return false;
        }

        PowerData data = PowerData.get(server);
        int level = data.getLevel(player.getUUID(), skill);
        if (level >= Skill.MAX_LEVEL) {
            fail(player, skill.label() + " já está no nível máximo.");
            return false;
        }

        List<String> missing = Costs.missing(skill, level,
                s -> data.getLevel(player.getUUID(), s), data.total(player.getUUID()));
        if (!missing.isEmpty()) {
            fail(player, "Requisitos pendentes: " + String.join("; ", missing));
            return false;
        }

        Costs.Cost cost = Costs.costFor(skill, level);
        Inventory inv = player.getInventory();
        int have = countItem(inv, cost.item());
        if (have < cost.count()) {
            fail(player, "Faltam itens: precisa de " + cost.count() + "x " + itemName(cost.item())
                    + " (você tem " + have + ").");
            return false;
        }
        if (player.experienceLevel < cost.xpLevels()) {
            fail(player, "Falta XP: precisa de " + cost.xpLevels() + " níveis (você tem "
                    + player.experienceLevel + ").");
            return false;
        }

        removeItem(inv, cost.item(), cost.count());
        player.giveExperienceLevels(-cost.xpLevels());
        data.setLevel(player.getUUID(), skill, level + 1);
        applyAll(player);
        if (skill == Skill.VIDA) {
            player.setHealth(player.getMaxHealth());
        }
        sync(player);

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal(skill.label() + " subiu para o nível " + (level + 1)
                + "! (" + skill.effectText(level + 1) + ")").withStyle(ChatFormatting.GREEN));
        return true;
    }

    private static void fail(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    /** Reaplica os modificadores de atributo do jogador. */
    public static void applyAll(ServerPlayer player) {
        MinecraftServer server = server(player);
        if (server == null) return;
        PowerData data = PowerData.get(server);
        for (Skill skill : Skill.values()) {
            if (!skill.isAttribute()) continue;
            AttributeInstance inst = player.getAttribute(skill.attribute());
            if (inst == null) continue;
            inst.removeModifier(skill.modifierId());
            int level = data.getLevel(player.getUUID(), skill);
            if (level > 0) {
                inst.addPermanentModifier(new AttributeModifier(
                        skill.modifierId(), skill.value(level), skill.operation()));
            }
        }
    }

    /** Envia os níveis atuais para o cliente do jogador. */
    public static void sync(ServerPlayer player) {
        MinecraftServer server = server(player);
        if (server == null) return;
        SyncPayload payload = new SyncPayload(PowerData.get(server).snapshot(player.getUUID()));
        player.connection.send(new ClientboundCustomPayloadPacket(payload));
    }

    public static String itemName(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    private static int countItem(Inventory inv, Item item) {
        int total = 0;
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    private static void removeItem(Inventory inv, Item item, int amount) {
        int left = amount;
        for (int i = 0; i < MAIN_SLOTS && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        inv.setChanged();
    }
}
