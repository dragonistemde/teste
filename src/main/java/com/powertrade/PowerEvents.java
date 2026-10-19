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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Arrays;
import java.util.List;

@EventBusSubscriber(modid = PowerTradeMod.MOD_ID)
public class PowerEvents {
    /** Evita recursão quando o próprio dano extra dispara os eventos de dano. */
    private static boolean inExtraDamage = false;

    // ---------- Jogador entra / renasce ----------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerLogic.applyAll(player);
            PowerLogic.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerLogic.applyAll(player);
            player.setHealth(player.getMaxHealth());
            PowerLogic.sync(player);
        }
    }

    // ---------- Ferraria ----------

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Forging.forge(player, event.getCrafting());
        }
    }

    // ---------- Resistências (quem sofre o dano) ----------

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        MinecraftServer server = PowerLogic.server(victim);
        if (server == null) return;
        PowerData data = PowerData.get(server);
        DamageSource source = event.getSource();

        double factor = 1.0;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            factor *= 1.0 - resist(data, victim, Skill.RESIST_MAGICA);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            factor *= 1.0 - resist(data, victim, Skill.RESIST_EXPLOSAO);
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            factor *= 1.0 - resist(data, victim, Skill.RESIST_FOGO);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            factor *= 1.0 - resist(data, victim, Skill.RESIST_PROJETIL);
        }
        if (factor < 1.0) {
            event.setAmount((float) (event.getAmount() * factor));
        }
    }

    private static double resist(PowerData data, ServerPlayer player, Skill skill) {
        return Math.min(0.75, skill.value(data.getLevel(player.getUUID(), skill)));
    }

    // ---------- Dano mágico e em área (quem causa o dano) ----------

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (inExtraDamage) return;
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
        if (source.getDirectEntity() != attacker) return; // só golpes corpo a corpo
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;

        MinecraftServer server = level.getServer();
        PowerData data = PowerData.get(server);
        int magicLvl = data.getLevel(attacker.getUUID(), Skill.DANO_MAGICO);
        int areaLvl = data.getLevel(attacker.getUUID(), Skill.DANO_AREA);
        if (magicLvl <= 0 && areaLvl <= 0) return;

        inExtraDamage = true;
        try {
            if (magicLvl > 0) {
                target.invulnerableTime = 0;
                target.hurtServer(level, level.damageSources().magic(), (float) Skill.DANO_MAGICO.value(magicLvl));
            }
            if (areaLvl > 0) {
                double radius = 2.0 + areaLvl * 0.07;
                float splash = (float) (event.getNewDamage() * Skill.DANO_AREA.value(areaLvl));
                if (splash > 0.0f) {
                    List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                            target.getBoundingBox().inflate(radius),
                            e -> e != target && e != attacker && e.isAlive()
                                    && !(e instanceof Player)
                                    && !e.isAlliedTo(attacker)
                                    && !(e instanceof TamableAnimal pet && pet.isOwnedBy(attacker)));
                    for (LivingEntity e : nearby) {
                        e.hurtServer(level, level.damageSources().playerAttack(attacker), splash);
                    }
                }
            }
        } finally {
            inExtraDamage = false;
        }
    }

    // ---------- Comandos (atalho de texto; a tela principal é no cliente) ----------

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("poder")
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("comprar")
                        .then(Commands.argument("habilidade", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Skill.values()).map(Skill::id), b))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    return PowerLogic.buy(player, StringArgumentType.getString(ctx, "habilidade")) ? 1 : 0;
                                }))));
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PowerData data = PowerData.get(source.getServer());

        player.sendSystemMessage(Component.literal("=== Árvore de Poder (poder total "
                + data.total(player.getUUID()) + ") ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("Dica: abra a tela com a tecla K ou o botão Poder no inventário.")
                .withStyle(ChatFormatting.DARK_GRAY));

        for (Branch branch : Branch.values()) {
            player.sendSystemMessage(Component.literal(branch.label()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            for (Skill skill : Skill.ofBranch(branch)) {
                int level = data.getLevel(player.getUUID(), skill);
                MutableComponent line = Component.literal(" • " + skill.label() + " ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("[" + level + "/" + Skill.MAX_LEVEL + "] ").withStyle(ChatFormatting.WHITE));
                if (level >= Skill.MAX_LEVEL) {
                    line.append(Component.literal("MAX").withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    Costs.Cost cost = Costs.costFor(skill, level);
                    line.append(Component.literal(cost.count() + "x " + PowerLogic.itemName(cost.item())
                            + " + " + cost.xpLevels() + " XP").withStyle(ChatFormatting.GRAY));
                    String cmd = "/poder comprar " + skill.id();
                    line.withStyle(s -> s
                            .withClickEvent(new ClickEvent.RunCommand(cmd))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(skill.description()))));
                }
                player.sendSystemMessage(line);
            }
        }
        return 1;
    }
}
