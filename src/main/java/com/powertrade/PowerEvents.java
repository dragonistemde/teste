package com.powertrade;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Arrays;

@EventBusSubscriber(modid = PowerTradeMod.MOD_ID)
public class PowerEvents {
    private static final int MAIN_SLOTS = 36;

    // ---------- Eventos ----------

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAll(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAll(player);
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---------- Comandos ----------

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("poder")
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("comprar")
                        .then(Commands.argument("habilidade", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Skill.values()).map(Skill::id), b))
                                .executes(ctx -> buy(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "habilidade"))))));
    }

    private static boolean isUnlocked(PowerData data, ServerPlayer player, Skill skill) {
        return skill.parent() == null
                || data.getLevel(player.getUUID(), skill.parent()) >= Skill.UNLOCK_LEVEL;
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PowerData data = PowerData.get(source.getServer());

        player.sendSystemMessage(Component.literal("=== Árvore de Poder ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        for (Skill skill : Skill.values()) {
            int level = data.getLevel(player.getUUID(), skill);
            String indent = skill.depth() == 0 ? "" : "  ".repeat(skill.depth() - 1) + "└ ";
            MutableComponent line = Component.literal(indent + skill.displayName() + " ")
                    .withStyle(ChatFormatting.YELLOW);

            if (!isUnlocked(data, player, skill)) {
                line.append(Component.literal("[bloqueado: " + skill.parent().displayName()
                        + " nv " + Skill.UNLOCK_LEVEL + "]").withStyle(ChatFormatting.DARK_GRAY));
            } else if (level >= Skill.MAX_LEVEL) {
                line.append(Component.literal("[" + level + "/" + Skill.MAX_LEVEL + "] MAX")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            } else {
                Skill.Cost cost = skill.costFor(level);
                line.append(Component.literal("[" + level + "/" + Skill.MAX_LEVEL + "] ")
                        .withStyle(ChatFormatting.AQUA));
                line.append(Component.literal("próximo: " + cost.count() + "x " + itemName(cost.item())
                        + " (" + skill.bonusText() + ")").withStyle(ChatFormatting.GRAY));
                String cmd = "/poder comprar " + skill.id();
                line.withStyle(s -> s
                        .withClickEvent(new ClickEvent.RunCommand(cmd))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Clique para comprar"))));
            }
            player.sendSystemMessage(line);
        }
        player.sendSystemMessage(Component.literal("Use /poder comprar <habilidade> ou clique na linha.")
                .withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static int buy(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Skill skill = Skill.byId(id);
        if (skill == null) {
            source.sendFailure(Component.literal("Habilidade desconhecida: " + id));
            return 0;
        }

        PowerData data = PowerData.get(source.getServer());
        if (!isUnlocked(data, player, skill)) {
            source.sendFailure(Component.literal(skill.displayName() + " está bloqueada: evolua "
                    + skill.parent().displayName() + " até o nível " + Skill.UNLOCK_LEVEL + "."));
            return 0;
        }

        int level = data.getLevel(player.getUUID(), skill);
        if (level >= Skill.MAX_LEVEL) {
            source.sendFailure(Component.literal(skill.displayName() + " já está no nível máximo."));
            return 0;
        }

        Skill.Cost cost = skill.costFor(level);
        Inventory inv = player.getInventory();
        int have = countItem(inv, cost.item());
        if (have < cost.count()) {
            source.sendFailure(Component.literal("Faltam itens: precisa de " + cost.count() + "x "
                    + itemName(cost.item()) + " (você tem " + have + ")."));
            return 0;
        }

        removeItem(inv, cost.item(), cost.count());
        data.setLevel(player.getUUID(), skill, level + 1);
        applyAll(player);
        if (skill == Skill.VIDA) {
            player.setHealth(player.getMaxHealth());
        }

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal(skill.displayName() + " subiu para o nível "
                + (level + 1) + "! (" + skill.bonusText() + ")").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    // ---------- Lógica ----------

    /** Reaplica todos os bônus do jogador como modificadores de atributo. */
    public static void applyAll(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PowerData data = PowerData.get(server);
        for (Skill skill : Skill.values()) {
            AttributeInstance inst = player.getAttribute(skill.attribute());
            if (inst == null) continue;
            inst.removeModifier(skill.modifierId());
            int level = data.getLevel(player.getUUID(), skill);
            if (level > 0) {
                inst.addPermanentModifier(new AttributeModifier(
                        skill.modifierId(), skill.perLevel() * level, skill.operation()));
            }
        }
    }

    private static String itemName(Item item) {
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
