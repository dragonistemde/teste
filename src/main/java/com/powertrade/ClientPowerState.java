package com.powertrade;

import java.util.HashMap;
import java.util.Map;

/**
 * Cópia dos níveis do jogador no cliente, atualizada por pacote do servidor.
 * Classe Java pura (sem classes do cliente), segura de carregar em servidor dedicado.
 */
public final class ClientPowerState {
    private ClientPowerState() {}

    private static volatile Map<String, Integer> levels = new HashMap<>();

    public static void set(Map<String, Integer> newLevels) {
        levels = new HashMap<>(newLevels);
    }

    public static int level(Skill skill) {
        return levels.getOrDefault(skill.id(), 0);
    }

    public static int total() {
        int sum = 0;
        for (int v : levels.values()) sum += v;
        return sum;
    }

    public static int branchTotal(Branch branch) {
        int sum = 0;
        for (Skill s : Skill.ofBranch(branch)) sum += level(s);
        return sum;
    }
}
