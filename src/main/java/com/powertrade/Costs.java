package com.powertrade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Custos e requisitos. Usado no servidor (validação) e no cliente (exibição). */
public final class Costs {
    private Costs() {}

    public record Cost(Item item, int count, int xpLevels, int requiredTotal) {}

    private static final int LEVELS_PER_TIER = 6;
    private static final Item[] TIER_ITEMS = {
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.NETHERITE_INGOT, Items.NETHER_STAR
    };
    private static final double[] TIER_BASE = {8, 7, 4, 2, 1};
    private static final double GROWTH = 1.22;
    /** Poder total (soma de todos os níveis) exigido por tier: 0, 25, 50, 75, 100. */
    private static final int TOTAL_PER_TIER = 25;

    /** Custo do próximo nível, dado o nível atual (0-based). */
    public static Cost costFor(Skill skill, int currentLevel) {
        int tier = Math.min(currentLevel / LEVELS_PER_TIER, TIER_ITEMS.length - 1);
        int within = currentLevel % LEVELS_PER_TIER;
        int count = (int) Math.max(1, Math.round(TIER_BASE[tier] * Math.pow(GROWTH, within) * skill.weight()));
        int xp = 1 + currentLevel / 3;
        return new Cost(TIER_ITEMS[tier], count, xp, tier * TOTAL_PER_TIER);
    }

    /** Lista o que falta para comprar o próximo nível (vazia = requisitos atendidos). */
    public static List<String> missing(Skill skill, int currentLevel, ToIntFunction<Skill> levelOf, int totalPower) {
        List<String> out = new ArrayList<>();
        for (Skill.Req r : skill.requirements()) {
            int have = levelOf.applyAsInt(r.skill());
            if (have < r.level()) {
                out.add(r.skill().label() + " nv " + r.level() + " (você: " + have + ")");
            }
        }
        Cost c = costFor(skill, currentLevel);
        if (totalPower < c.requiredTotal()) {
            out.add("Poder total " + c.requiredTotal() + " (você: " + totalPower + ")");
        }
        return out;
    }
}
