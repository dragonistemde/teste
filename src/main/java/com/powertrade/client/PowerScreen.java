package com.powertrade.client;

import com.powertrade.BuyPayload;
import com.powertrade.Branch;
import com.powertrade.ClientPowerState;
import com.powertrade.Costs;
import com.powertrade.Skill;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela da árvore de poder: abas por ramo à esquerda, habilidades no centro, detalhes à direita.
 * Só desenha retângulos e texto (barato) e usa um único passe no inventário por quadro.
 */
public class PowerScreen extends Screen {
    private static final int W = 420;
    private static final int H = 256;

    private static final int ROW_H = 24;
    private static final int ROW_GAP = 2;
    private static final int ROWS_W = 190;
    private static final int TAB_W = 80;
    private static final int TAB_H = 24;
    private static final int TAB_GAP = 4;
    private static final int CONTENT_TOP = 44;

    private static final int C_BG = 0xF0181620;
    private static final int C_BORDER = 0xFF3A3560;
    private static final int C_ROW = 0xFF24212F;
    private static final int C_ROW_HOVER = 0xFF2F2B3E;
    private static final int C_ROW_LOCKED = 0xFF1B1923;
    private static final int C_ROW_MAX = 0xFF2D2342;
    private static final int C_TEXT = 0xFFFFFFFF;
    private static final int C_DIM = 0xFF8D88A6;
    private static final int C_GOOD = 0xFF6FE08A;
    private static final int C_BAD = 0xFFE86B6B;
    private static final int C_GOLD = 0xFFFFD36B;

    private Branch tab = Branch.COMBATE;
    private int left;
    private int top;

    private final Map<Item, Integer> held = new HashMap<>();
    private final Map<Item, ItemStack> icons = new HashMap<>();

    public PowerScreen() {
        super(Component.literal("Árvore de Poder"));
    }

    // ---------- geometria ----------

    private int rowsX() { return left + 96; }
    private int detailX() { return rowsX() + ROWS_W + 8; }
    private int detailW() { return left + W - 8 - detailX(); }
    private int tabY(int i) { return top + CONTENT_TOP + i * (TAB_H + TAB_GAP); }
    private int rowY(int i) { return top + CONTENT_TOP + i * (ROW_H + ROW_GAP); }

    private int rowAt(double mx, double my, int count) {
        if (mx < rowsX() || mx >= rowsX() + ROWS_W) return -1;
        int rel = (int) my - (top + CONTENT_TOP);
        if (rel < 0) return -1;
        int idx = rel / (ROW_H + ROW_GAP);
        if (idx >= count || rel % (ROW_H + ROW_GAP) >= ROW_H) return -1;
        return idx;
    }

    private int tabAt(double mx, double my) {
        if (mx < left + 8 || mx >= left + 8 + TAB_W) return -1;
        Branch[] all = Branch.values();
        for (int i = 0; i < all.length; i++) {
            int y = tabY(i);
            if (my >= y && my < y + TAB_H) return i;
        }
        return -1;
    }

    // ---------- estado ----------

    private void countInventory() {
        held.clear();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) held.merge(s.getItem(), s.getCount(), Integer::sum);
        }
    }

    private int xpLevels() {
        return minecraft.player == null ? 0 : minecraft.player.experienceLevel;
    }

    private ItemStack icon(Item item) {
        return icons.computeIfAbsent(item, i -> new ItemStack(i));
    }

    private List<String> missingFor(Skill skill) {
        int level = ClientPowerState.level(skill);
        if (level >= Skill.MAX_LEVEL) return List.of();
        return Costs.missing(skill, level, ClientPowerState::level, ClientPowerState.total());
    }

    // ---------- desenho ----------

    private void box(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int fill, int border) {
        g.fill(x1, y1, x2, y2, border);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
    }

    private void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(font, Component.literal(s), x, y, color, true);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && font.width(Component.literal(candidate)) > maxWidth) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        super.extractRenderState(g, mx, my, partialTick);

        left = (width - W) / 2;
        top = (height - H) / 2;
        countInventory();

        List<Skill> skills = Skill.ofBranch(tab);
        int hovered = rowAt(mx, my, skills.size());
        int hoveredTab = tabAt(mx, my);

        box(g, left, top, left + W, top + H, C_BG, C_BORDER);

        // cabeçalho
        text(g, "Árvore de Poder", left + 10, top + 9, C_GOLD);
        String keyName = PowerTradeClient.OPEN_KEY.getTranslatedKeyMessage().getString();
        text(g, "Poder total: " + ClientPowerState.total() + "    XP: " + xpLevels() + " níveis    [" + keyName + "] fecha",
                left + 10, top + 24, C_DIM);

        drawTabs(g, hoveredTab);
        drawRows(g, skills, hovered);
        drawDetails(g, hovered >= 0 ? skills.get(hovered) : null);
    }

    private void drawTabs(GuiGraphicsExtractor g, int hoveredTab) {
        Branch[] all = Branch.values();
        for (int i = 0; i < all.length; i++) {
            Branch b = all[i];
            int x = left + 8;
            int y = tabY(i);
            boolean selected = b == tab;
            int fill = selected ? 0xFF2F2B3E : (i == hoveredTab ? 0xFF272433 : C_ROW);
            box(g, x, y, x + TAB_W, y + TAB_H, fill, selected ? b.color() : C_BORDER);
            g.fill(x + 1, y + 1, x + 4, y + TAB_H - 1, b.color());
            text(g, b.label(), x + 9, y + 3, selected ? C_TEXT : C_DIM);
            text(g, "nível " + ClientPowerState.branchTotal(b), x + 9, y + 13, C_DIM);
        }
    }

    private void drawRows(GuiGraphicsExtractor g, List<Skill> skills, int hovered) {
        int x = rowsX();
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            int y = rowY(i);
            int level = ClientPowerState.level(skill);
            boolean max = level >= Skill.MAX_LEVEL;
            List<String> missing = missingFor(skill);
            boolean locked = !max && !missing.isEmpty();

            Costs.Cost cost = max ? null : Costs.costFor(skill, level);
            int have = cost == null ? 0 : held.getOrDefault(cost.item(), 0);
            boolean afford = cost != null && !locked && have >= cost.count() && xpLevels() >= cost.xpLevels();

            int fill = max ? C_ROW_MAX : locked ? C_ROW_LOCKED : (i == hovered ? C_ROW_HOVER : C_ROW);
            int border = afford ? C_GOOD : max ? 0xFFB08CFF : C_BORDER;
            box(g, x, y, x + ROWS_W, y + ROW_H, fill, border);

            text(g, skill.label(), x + 6, y + 3, locked ? 0xFF6A6680 : C_TEXT);

            // barra de nível
            int barX = x + 6;
            int barY = y + 15;
            int barW = 100;
            g.fill(barX, barY, barX + barW, barY + 4, 0xFF0E0D14);
            int filled = barW * level / Skill.MAX_LEVEL;
            if (filled > 0) g.fill(barX, barY, barX + filled, barY + 4, skill.branch().color());

            text(g, level + "/" + Skill.MAX_LEVEL, x + 112, y + 8, locked ? 0xFF6A6680 : C_DIM);

            if (max) {
                text(g, "MAX", x + 152, y + 8, 0xFFB08CFF);
            } else if (locked) {
                text(g, "bloqueado", x + 140, y + 8, 0xFF6A6680);
            } else {
                g.item(icon(cost.item()), x + 142, y + 4);
                text(g, "x" + cost.count(), x + 161, y + 8, have >= cost.count() ? C_GOOD : C_BAD);
            }
        }
    }

    private void drawDetails(GuiGraphicsExtractor g, Skill skill) {
        int x = detailX();
        int y0 = top + CONTENT_TOP;
        int w = detailW();
        box(g, x, y0, x + w, top + H - 8, 0xFF1E1B28, C_BORDER);

        int tx = x + 6;
        int ty = y0 + 6;
        int inner = w - 12;

        if (skill == null) {
            for (String line : wrap("Passe o mouse sobre uma habilidade para ver os detalhes. Clique para comprar o próximo nível.", inner)) {
                text(g, line, tx, ty, C_DIM);
                ty += 10;
            }
            return;
        }

        int level = ClientPowerState.level(skill);
        text(g, skill.label(), tx, ty, skill.branch().color());
        ty += 12;
        for (String line : wrap(skill.description(), inner)) {
            text(g, line, tx, ty, C_DIM);
            ty += 9;
        }
        ty += 4;

        text(g, "Nível " + level + "/" + Skill.MAX_LEVEL, tx, ty, C_TEXT);
        ty += 11;
        for (String line : wrap("Atual: " + (level > 0 ? skill.effectText(level) : "nenhum"), inner)) {
            text(g, line, tx, ty, C_TEXT);
            ty += 9;
        }
        if (level >= Skill.MAX_LEVEL) return;

        for (String line : wrap("Próximo: " + skill.effectText(level + 1), inner)) {
            text(g, line, tx, ty, C_GOOD);
            ty += 9;
        }
        ty += 4;

        Costs.Cost cost = Costs.costFor(skill, level);
        int have = held.getOrDefault(cost.item(), 0);
        text(g, "Custo:", tx, ty, C_TEXT);
        ty += 10;
        g.item(icon(cost.item()), tx, ty - 2);
        text(g, cost.count() + "x (tem " + have + ")", tx + 20, ty + 2, have >= cost.count() ? C_GOOD : C_BAD);
        ty += 16;
        text(g, cost.xpLevels() + " níveis de XP", tx, ty, xpLevels() >= cost.xpLevels() ? C_GOOD : C_BAD);
        ty += 13;

        text(g, "Requisitos:", tx, ty, C_TEXT);
        ty += 10;
        boolean any = false;
        for (Skill.Req r : skill.requirements()) {
            int have2 = ClientPowerState.level(r.skill());
            boolean ok = have2 >= r.level();
            text(g, (ok ? "ok  " : "x   ") + r.skill().label() + " " + r.level(), tx, ty, ok ? C_GOOD : C_BAD);
            ty += 9;
            any = true;
        }
        if (cost.requiredTotal() > 0) {
            boolean ok = ClientPowerState.total() >= cost.requiredTotal();
            text(g, (ok ? "ok  " : "x   ") + "Poder total " + cost.requiredTotal(), tx, ty, ok ? C_GOOD : C_BAD);
            any = true;
        }
        if (!any) {
            text(g, "nenhum", tx, ty, C_DIM);
        }
    }

    // ---------- entrada ----------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();

            int t = tabAt(mx, my);
            if (t >= 0) {
                tab = Branch.values()[t];
                return true;
            }

            List<Skill> skills = Skill.ofBranch(tab);
            int idx = rowAt(mx, my, skills.size());
            if (idx >= 0) {
                Skill skill = skills.get(idx);
                if (ClientPowerState.level(skill) < Skill.MAX_LEVEL && missingFor(skill).isEmpty()) {
                    ClientPacketListener conn = minecraft.getConnection();
                    if (conn != null) {
                        conn.send(new ServerboundCustomPayloadPacket(new BuyPayload(skill.id())));
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (PowerTradeClient.OPEN_KEY.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
