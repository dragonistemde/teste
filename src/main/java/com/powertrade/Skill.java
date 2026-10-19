package com.powertrade;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Todas as habilidades. Habilidades de atributo viram modificadores de atributo do jogador;
 * as "custom" (attribute == null) são aplicadas por eventos em {@link PowerEvents} / {@link Forging}.
 * Requisitos só podem apontar para habilidades declaradas ANTES na lista.
 */
public enum Skill {
    // ---------------- COMBATE ----------------
    DANO("dano", "Dano de Ataque", Branch.COMBATE,
            Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE, 0.8, 1, "", " de dano base",
            "Aumenta o dano base de todos os seus ataques.", 1.0),
    ATAQUE("ataque", "Velocidade de Ataque", Branch.COMBATE,
            Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE, 0.12, 1, "", " de vel. de ataque",
            "Recupera o golpe mais rápido.", 1.0),
    ALCANCE("alcance", "Alcance de Combate", Branch.COMBATE,
            Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_VALUE, 0.08, 1, "", " de alcance",
            "Acerta inimigos de mais longe.", 1.1,
            new Req("ATAQUE", 4)),
    DANO_MAGICO("dano_magico", "Dano Mágico", Branch.COMBATE,
            null, null, 0.5, 1, "", " de dano mágico por golpe",
            "Cada golpe causa dano mágico extra que ignora armadura.", 1.3,
            new Req("DANO", 5)),
    DANO_AREA("dano_area", "Dano em Área", Branch.COMBATE,
            null, null, 0.015, 100, "%", " do dano em área",
            "Seus golpes também ferem inimigos próximos (raio cresce com o nível).", 1.3,
            new Req("ATAQUE", 5), new Req("DANO", 5)),

    // ---------------- DEFESA ----------------
    VIDA("vida", "Vida Máxima", Branch.DEFESA,
            Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_VALUE, 1.0, 1, "", " de vida máxima",
            "Mais pontos de vida (2 pontos = 1 coração).", 1.0),
    ARMADURA("armadura", "Armadura", Branch.DEFESA,
            Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE, 0.5, 1, "", " de armadura",
            "Reduz dano físico.", 1.0),
    TENACIDADE("tenacidade", "Tenacidade", Branch.DEFESA,
            Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE, 0.25, 1, "", " de tenacidade",
            "Armadura funciona melhor contra golpes fortes.", 1.1,
            new Req("ARMADURA", 4)),
    KNOCKBACK("knockback", "Firmeza", Branch.DEFESA,
            Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE, 0.02, 100, "%", " resist. a knockback",
            "Difícil de empurrar.", 1.0,
            new Req("ARMADURA", 3)),
    RESIST_MAGICA("resist_magica", "Resistência Mágica", Branch.DEFESA,
            null, null, 0.02, 100, "%", " menos dano mágico",
            "Reduz dano mágico (poções de dano, bruxas, evocadores).", 1.3,
            new Req("VIDA", 3)),
    RESIST_EXPLOSAO("resist_explosao", "Resist. a Explosão", Branch.DEFESA,
            null, null, 0.02, 100, "%", " menos dano de explosão",
            "Reduz dano de creepers, TNT e cristais.", 1.2,
            new Req("ARMADURA", 3)),
    RESIST_FOGO("resist_fogo", "Resist. a Fogo", Branch.DEFESA,
            null, null, 0.02, 100, "%", " menos dano de fogo e lava",
            "Reduz dano de fogo, lava e queimadura.", 1.2,
            new Req("VIDA", 3)),
    RESIST_PROJETIL("resist_projetil", "Resist. a Projéteis", Branch.DEFESA,
            null, null, 0.015, 100, "%", " menos dano de projéteis",
            "Reduz dano de flechas, tridentes e bolas de fogo.", 1.2,
            new Req("ARMADURA", 5)),

    // ---------------- UTILIDADE ----------------
    VELOCIDADE("velocidade", "Velocidade", Branch.UTILIDADE,
            Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.01, 100, "%", " de velocidade",
            "Corre mais rápido.", 1.0),
    PULO("pulo", "Força de Pulo", Branch.UTILIDADE,
            Attributes.JUMP_STRENGTH, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.01, 100, "%", " de pulo",
            "Pula mais alto.", 1.0,
            new Req("VELOCIDADE", 3)),
    MINERACAO("mineracao", "Mineração", Branch.UTILIDADE,
            Attributes.BLOCK_BREAK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.03, 100, "%", " de vel. de mineração",
            "Quebra blocos mais rápido.", 1.0),
    SORTE("sorte", "Sorte", Branch.UTILIDADE,
            Attributes.LUCK, AttributeModifier.Operation.ADD_VALUE, 0.2, 1, "", " de sorte",
            "Melhora drops de baús e pesca.", 1.2,
            new Req("MINERACAO", 5)),
    QUEDA("queda", "Queda Segura", Branch.UTILIDADE,
            Attributes.SAFE_FALL_DISTANCE, AttributeModifier.Operation.ADD_VALUE, 0.4, 1, "", " blocos de queda segura",
            "Cai de mais alto sem se machucar.", 1.0,
            new Req("PULO", 3)),

    // ---------------- FERRARIA ----------------
    FERRARIA("ferraria", "Ferraria", Branch.FERRARIA,
            null, null, 0.035, 100, "%", " de durabilidade em itens forjados",
            "Itens que você fabrica ficam mais duráveis. Para forjar sem defeito: ferro nv 3, diamante nv 8, netherite nv 15.", 1.0),
    FORJA_ARMAS("forja_armas", "Forja de Armas", Branch.FERRARIA,
            null, null, 0.4, 1, "", " de dano em espadas e machados forjados",
            "Espadas e machados que você fabrica ganham dano extra permanente.", 1.2,
            new Req("FERRARIA", 3), new Req("DANO", 3)),
    FORJA_ARMADURAS("forja_armaduras", "Forja de Armaduras", Branch.FERRARIA,
            null, null, 0.2, 1, "", " de armadura por peça forjada",
            "Armaduras que você fabrica ganham armadura e tenacidade extras.", 1.2,
            new Req("FERRARIA", 3), new Req("ARMADURA", 3));

    public static final int MAX_LEVEL = 30;

    /** Requisito: outra habilidade (pelo nome do enum) em nível mínimo. Guardado por nome para evitar referência circular na inicialização. */
    public record Req(String skillName, int level) {
        public Skill skill() { return Skill.valueOf(skillName); }
    }

    private final String id;
    private final String label;
    private final Branch branch;
    private final Holder<Attribute> attribute;
    private final AttributeModifier.Operation operation;
    private final double perLevel;
    private final int displayMult;
    private final String unit;
    private final String what;
    private final String description;
    private final double weight;
    private final Req[] requirements;

    Skill(String id, String label, Branch branch, Holder<Attribute> attribute,
          AttributeModifier.Operation operation, double perLevel, int displayMult, String unit, String what,
          String description, double weight, Req... requirements) {
        this.id = id;
        this.label = label;
        this.branch = branch;
        this.attribute = attribute;
        this.operation = operation;
        this.perLevel = perLevel;
        this.displayMult = displayMult;
        this.unit = unit;
        this.what = what;
        this.description = description;
        this.weight = weight;
        this.requirements = requirements;
    }

    public String id() { return id; }
    public String label() { return label; }
    public Branch branch() { return branch; }
    public Holder<Attribute> attribute() { return attribute; }
    public AttributeModifier.Operation operation() { return operation; }
    public double perLevel() { return perLevel; }
    public String description() { return description; }
    public double weight() { return weight; }
    public Req[] requirements() { return requirements; }
    public boolean isAttribute() { return attribute != null; }

    /** Valor bruto acumulado no nível dado (ex.: 0.8 * nível). */
    public double value(int level) { return perLevel * level; }

    /** Texto do efeito no nível dado, ex.: "+4,8 de dano base". */
    public String effectText(int level) {
        return "+" + fmt(value(level) * displayMult) + unit + what;
    }

    public Identifier modifierId() {
        return Identifier.fromNamespaceAndPath(PowerTradeMod.MOD_ID, "skill_" + id);
    }

    public static Skill byId(String id) {
        for (Skill s : values()) {
            if (s.id.equalsIgnoreCase(id)) return s;
        }
        return null;
    }

    public static List<Skill> ofBranch(Branch branch) {
        List<Skill> out = new ArrayList<>();
        for (Skill s : values()) {
            if (s.branch == branch) out.add(s);
        }
        return out;
    }

    public static String fmt(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}
