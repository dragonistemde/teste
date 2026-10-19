package com.powertrade;

public enum Branch {
    COMBATE("Combate", 0xFFE5534B),
    DEFESA("Defesa", 0xFF4B9BE5),
    UTILIDADE("Utilidade", 0xFF5BC470),
    FERRARIA("Ferraria", 0xFFE0A030);

    private final String label;
    private final int color;

    Branch(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String label() { return label; }

    /** Cor ARGB. */
    public int color() { return color; }
}
