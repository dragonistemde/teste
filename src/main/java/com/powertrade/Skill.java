package com.powertrade;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Nós da árvore de poder. Cada ramo começa numa raiz; os filhos só liberam
 * quando o pai chega a {@link #UNLOCK_LEVEL}.
 */
public enum Skill {
    // Ramo Combate
    DANO("dano", "Dano de Ataque", Attributes.ATTACK_DAMAGE, 1.0, AttributeModifier.Operation.ADD_VALUE, "+1 de dano base", null),
    ATAQUE("ataque", "Velocidade de Ataque", Attributes.ATTACK_SPEED, 0.2, AttributeModifier.Operation.ADD_VALUE, "+0.2 vel. de ataque", DANO),
    ALCANCE("alcance", "Alcance de Combate", Attributes.ENTITY_INTERACTION_RANGE, 0.15, AttributeModifier.Operation.ADD_VALUE, "+0.15 de alcance", ATAQUE),

    // Ramo Defesa
    VIDA("vida", "Vida Máxima", Attributes.MAX_HEALTH, 2.0, AttributeModifier.Operation.ADD_VALUE, "+1 coração", null),
    ARMADURA("armadura", "Armadura", Attributes.ARMOR, 1.0, AttributeModifier.Operation.ADD_VALUE, "+1 de armadura", VIDA),
    RESISTENCIA("resistencia", "Resistência a Knockback", Attributes.KNOCKBACK_RESISTANCE, 0.04, AttributeModifier.Operation.ADD_VALUE, "+4% resist. a knockback", ARMADURA),

    // Ramo Utilidade
    VELOCIDADE("velocidade", "Velocidade", Attributes.MOVEMENT_SPEED, 0.04, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "+4% de velocidade", null),
    PULO("pulo", "Força de Pulo", Attributes.JUMP_STRENGTH, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "+5% de pulo", VELOCIDADE),
    MINERACAO("mineracao", "Velocidade de Mineração", Attributes.BLOCK_BREAK_SPEED, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "+10% de mineração", VELOCIDADE),
    SORTE("sorte", "Sorte", Attributes.LUCK, 1.0, AttributeModifier.Operation.ADD_VALUE, "+1 de sorte", MINERACAO);

    public static final int MAX_LEVEL = 25;
    /** Nível que o pai precisa ter para liberar o filho. */
    public static final int UNLOCK_LEVEL = 3;

    private static final int LEVELS_PER_TIER = 5;

    // Escada de custos: a cada 5 níveis troca para um item mais raro.
    private static final Item[] TIER_ITEMS = {
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.NETHERITE_INGOT, Items.NETHER_STAR
    };
    private static final int[] TIER_BASE = {10, 12, 6, 3, 1};
    private static final int[] TIER_STEP = {5, 6, 3, 1, 1};

    public record Cost(Item item, int count) {}

    private final String id;
    private final String displayName;
    private final Holder<Attribute> attribute;
    private final double perLevel;
    private final AttributeModifier.Operation operation;
    private final String bonusText;
    private final Skill parent;
    private final int depth;

    Skill(String id, String displayName, Holder<Attribute> attribute, double perLevel,
          AttributeModifier.Operation operation, String bonusText, Skill parent) {
        this.id = id;
        this.displayName = displayName;
        this.attribute = attribute;
        this.perLevel = perLevel;
        this.operation = operation;
        this.bonusText = bonusText;
        this.parent = parent;
        this.depth = parent == null ? 0 : parent.depth + 1;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Holder<Attribute> attribute() { return attribute; }
    public double perLevel() { return perLevel; }
    public AttributeModifier.Operation operation() { return operation; }
    public String bonusText() { return bonusText; }
    public Skill parent() { return parent; }
    public int depth() { return depth; }

    public Identifier modifierId() {
        return Identifier.fromNamespaceAndPath(PowerTradeMod.MOD_ID, "skill_" + id);
    }

    /** Custo do próximo nível, dado o nível atual (0-based). Nós mais fundos na árvore custam mais. */
    public Cost costFor(int currentLevel) {
        int tier = Math.min(currentLevel / LEVELS_PER_TIER, TIER_ITEMS.length - 1);
        int within = currentLevel % LEVELS_PER_TIER;
        int base = TIER_BASE[tier] + TIER_STEP[tier] * within;
        int count = base + (base * depth) / 2;
        return new Cost(TIER_ITEMS[tier], count);
    }

    public static Skill byId(String id) {
        for (Skill s : values()) {
            if (s.id.equalsIgnoreCase(id)) return s;
        }
        return null;
    }
}
