
package com.seunome.jumpreset;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiJumpReset extends GuiScreen {

    private enum Kind { CHECK, MODE, SLIDER, CHOICE }

    private static final int PANEL_W = 336;
    private static final int COL_W = 156;
    private static final int ROW_H = 22;
    private static final int PAD = 8;

    private static final int ACCENT = 0xFF8E6BF0;
    private static final int ACCENT_DIM = 0xFFB9A8FF;
    private static final int PANEL_BG = 0xF00E0E14;
    private static final int ROW_BG = 0x22FFFFFF;
    private static final int TRACK_BG = 0xFF2A2A35;
    private static final int TEXT = 0xFFD5D5DD;
    private static final int TEXT_DIM = 0xFF70707C;
    private static final int GREEN = 0xFF4CD964;
    private static final int RED = 0xFFFF5A5A;

    private final List<Row> rows = new ArrayList<Row>();

    private Row dragging;
    private int page = 0;

    private int panelX;
    private int panelY;
    private int panelH;

    private static final class Row {
        final int id;
        final String name;
        final Kind kind;
        final int x;
        final int y;
        final int w;

        Row(int id, String name, Kind kind, int x, int y, int w) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.w = w;
        }
    }

    @Override
    public void initGui() {
        panelX = (width - PANEL_W) / 2;
        panelH = 224;
        panelY = (height - panelH) / 2;

        buildRows();
    }

    private void buildRows() {
        rows.clear();

        int left = panelX + PAD;
        int right = left + COL_W + 8;
        int top = panelY + 57;

        if (page == 0) {
            // JUMP RESET - configurações existentes
            add(0, "JumpReset", Kind.CHECK, left, 0, top);
            add(1, "Mode", Kind.MODE, left, 1, top);
            add(2, "Velocity", Kind.CHECK, left, 2, top);
            add(3, "Blatant KB", Kind.SLIDER, left, 3, top);
            add(4, "Anti Vertical", Kind.CHECK, left, 4, top);
            add(5, "AntiKB Hold", Kind.SLIDER, left, 5, top);
            add(9, "Miss Chance", Kind.SLIDER, left, 6, top);

            add(6, "Chance", Kind.SLIDER, right, 0, top);
            add(7, "Accuracy", Kind.SLIDER, right, 1, top);
            add(8, "Instant Jump", Kind.CHECK, right, 2, top);
            add(10, "Only Targeting", Kind.CHECK, right, 3, top);
            add(11, "Nearby Player", Kind.CHECK, right, 4, top);
            add(12, "Water Check", Kind.CHECK, right, 5, top);
            add(13, "Delay", Kind.SLIDER, right, 6, top);

        } else if (page == 1) {
            // WTAP
            add(100, "WTap", Kind.CHECK, left, 0, top);
            add(101, "Active Distance", Kind.SLIDER, left, 1, top);
            add(102, "Delay", Kind.SLIDER, right, 0, top);
            add(103, "Post Delay", Kind.SLIDER, right, 1, top);

        } else {
            // BLOCKHIT
            add(200, "BlockHit", Kind.CHECK, left, 0, top);
            add(201, "Mode", Kind.CHOICE, left, 1, top);
            add(202, "Min CPS", Kind.SLIDER, left, 2, top);
            add(203, "Max CPS", Kind.SLIDER, left, 3, top);

            add(204, "Randomization", Kind.CHOICE, right, 0, top);
            add(205, "Jitter", Kind.CHECK, right, 1, top);
            add(206, "Limit Items", Kind.CHECK, right, 2, top);
        }
    }

    private void add(int id, String name, Kind kind,
                     int x, int index, int top) {
        rows.add(new Row(
                id, name, kind, x,
                top + index * ROW_H, COL_W
        ));
    }

    // ---------------- VALORES ----------------

    private boolean getBool(int id) {
        switch (id) {
            case 0: return JumpResetMod.enabled;
            case 2: return JumpResetMod.velocityEnabled;
            case 4: return JumpResetMod.blatantAntiY;
            case 8: return JumpResetMod.instantJump;
            case 10: return JumpResetMod.onlyWhenTargeting;
            case 11: return JumpResetMod.nearbyPlayerCheck;
            case 12: return JumpResetMod.waterCheck;

            case 100: return CombatModules.wtapEnabled;
            case 200: return CombatModules.blockHitEnabled;
            case 205: return CombatModules.jitter;
            case 206: return CombatModules.limitItems;

            default: return false;
        }
    }

    private void setBool(int id, boolean value) {
        switch (id) {
            case 0: JumpResetMod.enabled = value; break;
            case 2: JumpResetMod.velocityEnabled = value; break;
            case 4: JumpResetMod.blatantAntiY = value; break;
            case 8: JumpResetMod.instantJump = value; break;
            case 10: JumpResetMod.onlyWhenTargeting = value; break;
            case 11: JumpResetMod.nearbyPlayerCheck = value; break;
            case 12: JumpResetMod.waterCheck = value; break;

            case 100: CombatModules.wtapEnabled = value; break;
            case 200: CombatModules.blockHitEnabled = value; break;
            case 205: CombatModules.jitter = value; break;
            case 206: CombatModules.limitItems = value; break;
        }
    }

    private int getSlider(int id) {
        switch (id) {
            case 3: return JumpResetMod.blatantKeep;
            case 5: return JumpResetMod.antikbHold;
            case 6: return JumpResetMod.chance;
            case 7: return JumpResetMod.accuracy;
            case 9: return JumpResetMod.velocityMissChance;
            case 13: return JumpResetMod.delay;

            case 101:
                return (int) Math.round(
                        CombatModules.activeDistance * 10.0D
                );

            case 102: return CombatModules.wtapDelay;
            case 103: return CombatModules.postDelay;

            case 202:
                return (int) Math.round(CombatModules.minCps * 10.0D);

            case 203:
                return (int) Math.round(CombatModules.maxCps * 10.0D);

            default: return 0;
        }
    }

    private void setSlider(int id, int value) {
        switch (id) {
            case 3: JumpResetMod.blatantKeep = value; break;
            case 5: JumpResetMod.antikbHold = value; break;
            case 6: JumpResetMod.chance = value; break;
            case 7: JumpResetMod.accuracy = value; break;
            case 9: JumpResetMod.velocityMissChance = value; break;
            case 13: JumpResetMod.delay = value; break;

            case 101:
                CombatModules.activeDistance = value / 10.0D;
                break;

            case 102:
                CombatModules.wtapDelay = value;
                break;

            case 103:
                CombatModules.postDelay = value;
                break;

            case 202:
                CombatModules.minCps = value / 10.0D;
                if (CombatModules.minCps > CombatModules.maxCps) {
                    CombatModules.maxCps = CombatModules.minCps;
                }
                break;

            case 203:
                CombatModules.maxCps = value / 10.0D;
                if (CombatModules.maxCps < CombatModules.minCps) {
                    CombatModules.minCps = CombatModules.maxCps;
                }
                break;
        }
    }

    private int sliderMin(int id) {
        if (id == 202 || id == 203) return 10;
        return 0;
    }

    private int sliderMax(int id) {
        if (id == 13) return 5;
        if (id == 5) return 10;
        if (id == 101) return 60;
        if (id == 102 || id == 103) return 1000;
        if (id == 202 || id == 203) return 300;
        return 100;
    }

    private String valueText(Row row) {
        switch (row.id) {
            case 0:
            case 2:
            case 4:
            case 8:
            case 10:
            case 11:
            case 12:
            case 100:
            case 200:
            case 205:
            case 206:
                return getBool(row.id) ? "ON" : "OFF";

            case 1:
                return modeLabel();

            case 3:
                return JumpResetMod.blatantKeep == 0
                        ? "ANTIKB"
                        : JumpResetMod.blatantKeep + "%";

            case 5: return JumpResetMod.antikbHold + "t";
            case 6: return JumpResetMod.chance + "%";
            case 7: return JumpResetMod.accuracy + "%";
            case 9: return JumpResetMod.velocityMissChance + "%";
            case 13: return JumpResetMod.delay + "t";

            case 101:
                return String.format("%.1f",
                        CombatModules.activeDistance);

            case 102: return CombatModules.wtapDelay + "ms";
            case 103: return CombatModules.postDelay + "ms";

            case 201:
                return CombatModules.blockHitMode.name();

            case 202:
                return String.format("%.1f", CombatModules.minCps);

            case 203:
                return String.format("%.1f", CombatModules.maxCps);

            case 204:
                return CombatModules.randomization.name();

            default: return "";
        }
    }

    private String modeLabel() {
        if (JumpResetMod.mode == JumpResetMod.Mode.BLATANT) {
            return "Blatant";
        }

        int lo = (int) Math.round(
                (JumpResetMod.mode.velocityFactor
                        - JumpResetMod.mode.randomizeRange) * 100.0D
        );

        int hi = (int) Math.round(
                (JumpResetMod.mode.velocityFactor
                        + JumpResetMod.mode.randomizeRange) * 100.0D
        );

        return JumpResetMod.mode.label + " " + lo + "-" + hi + "%";
    }

    private int valueColor(Row row) {
        if (row.kind == Kind.CHECK) {
            return getBool(row.id) ? GREEN : RED;
        }

        if (row.id == 3 && JumpResetMod.blatantKeep == 0) {
            return 0xFFFF9E42;
        }

        return ACCENT_DIM;
    }

    private void nextChoice(int id) {
        if (id == 201) {
            CombatModules.blockHitMode =
                    CombatModules.blockHitMode == CombatModules.BlockHitMode.AUTO
                            ? CombatModules.BlockHitMode.PREDICT
                            : CombatModules.BlockHitMode.AUTO;
        }

        if (id == 204) {
            CombatModules.randomization =
                    CombatModules.randomization.next();
        }
    }

    // ---------------- DESENHO ----------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x90000000);

        drawRect(panelX, panelY,
                panelX + PANEL_W, panelY + panelH, PANEL_BG);

        drawRect(panelX, panelY,
                panelX + PANEL_W, panelY + 2, ACCENT);

        drawString(fontRendererObj, "Jump Reset",
                panelX + PAD, panelY + 10, 0xFFFFFFFF);

        String ver = "v" + JumpResetMod.VERSION;

        drawString(fontRendererObj, ver,
                panelX + PANEL_W - PAD
                        - fontRendererObj.getStringWidth(ver),
                panelY + 10, TEXT_DIM);

        drawTabs();

        for (Row row : rows) {
            drawRect(row.x, row.y,
                    row.x + row.w, row.y + ROW_H - 3, ROW_BG);

            drawString(fontRendererObj, row.name,
                    row.x + 6, row.y + 5, TEXT);

            String value = valueText(row);

            drawString(fontRendererObj, value,
                    row.x + row.w - 6
                            - fontRendererObj.getStringWidth(value),
                    row.y + 5, valueColor(row));

            if (row.kind == Kind.SLIDER) {
                int left = row.x + 6;
                int right = row.x + row.w - 6;
                int trackY = row.y + ROW_H - 7;

                drawRect(left, trackY,
                        right, trackY + 2, TRACK_BG);

                float ratio = (float)
                        (getSlider(row.id) - sliderMin(row.id))
                        / (float)
                        (sliderMax(row.id) - sliderMin(row.id));

                ratio = Math.max(0.0F, Math.min(1.0F, ratio));

                int fill = left
                        + Math.round((right - left) * ratio);

                drawRect(left, trackY,
                        fill, trackY + 2, ACCENT);

                drawRect(fill - 1, trackY - 2,
                        fill + 2, trackY + 4, 0xFFFFFFFF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawTabs() {
        String[] names = {"J", "W", "B"};

        int tabW = 32;
        int gap = 5;
        int total = tabW * 3 + gap * 2;

        int startX = panelX + (PANEL_W - total) / 2;
        int y = panelY + 32;

        for (int i = 0; i < 3; i++) {
            int x = startX + i * (tabW + gap);

            int color = page == i
                    ? ACCENT
                    : ROW_BG;

            drawRect(x, y, x + tabW, y + 18, color);

            int textW = fontRendererObj.getStringWidth(names[i]);

            drawString(fontRendererObj, names[i],
                    x + (tabW - textW) / 2,
                    y + 5, 0xFFFFFFFF);
        }
    }

    // ---------------- CLIQUES ----------------

    @Override
    protected void mouseClicked(int mouseX, int mouseY,
                                int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) return;

        int tabW = 32;
        int gap = 5;
        int total = tabW * 3 + gap * 2;
        int startX = panelX + (PANEL_W - total) / 2;
        int tabY = panelY + 32;

        if (mouseY >= tabY && mouseY <= tabY + 18) {
            for (int i = 0; i < 3; i++) {
                int x = startX + i * (tabW + gap);

                if (mouseX >= x && mouseX <= x + tabW) {
                    page = i;
                    dragging = null;
                    buildRows();
                    return;
                }
            }
        }

        for (Row row : rows) {
            if (mouseX < row.x || mouseX > row.x + row.w) {
                continue;
            }

            if (mouseY < row.y
                    || mouseY >= row.y + ROW_H - 3) {
                continue;
            }

            if (row.kind == Kind.CHECK) {
                setBool(row.id, !getBool(row.id));

            } else if (row.kind == Kind.MODE) {
                JumpResetMod.mode = JumpResetMod.mode.next();

            } else if (row.kind == Kind.CHOICE) {
                nextChoice(row.id);

            } else {
                dragging = row;
                dragSlider(row, mouseX);
            }

            return;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY,
                                  int clickedMouseButton,
                                  long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY,
                clickedMouseButton, timeSinceLastClick);

        if (dragging != null) {
            dragSlider(dragging, mouseX);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void dragSlider(Row row, int mouseX) {
        int left = row.x + 6;
        int right = row.x + row.w - 6;

        float ratio = (float) (mouseX - left)
                / (float) (right - left);

        ratio = Math.max(0.0F, Math.min(1.0F, ratio));

        int value = sliderMin(row.id)
                + Math.round(ratio
                * (sliderMax(row.id) - sliderMin(row.id)));

        setSlider(row.id, value);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
            throws IOException {
        if (keyCode == Keyboard.KEY_Z
                || keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        JumpResetMod.saveConfig();
        CombatModules.saveConfig();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
