package com.seunome.jumpreset;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiJumpReset extends GuiScreen {

    private enum Kind { CHECK, MODE, SLIDER }

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
        rows.clear();
        panelX = (width - PANEL_W) / 2;
        panelH = 40 + 7 * ROW_H + 18;
        panelY = (height - panelH) / 2;

        int left = panelX + PAD;
        int right = left + COL_W + 8;
        int top = panelY + 38;

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
    }

    private void add(int id, String name, Kind kind, int x, int index, int top) {
        rows.add(new Row(id, name, kind, x, top + index * ROW_H, COL_W));
    }

    // ---------------- valores ----------------

    private boolean getBool(int id) {
        switch (id) {
            case 0: return JumpResetMod.enabled;
            case 2: return JumpResetMod.velocityEnabled;
            case 4: return JumpResetMod.blatantAntiY;
            case 8: return JumpResetMod.instantJump;
            case 10: return JumpResetMod.onlyWhenTargeting;
            case 11: return JumpResetMod.nearbyPlayerCheck;
            case 12: return JumpResetMod.waterCheck;
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
        }
    }

    private int sliderMax(int id) {
        if (id == 13) return 5;
        if (id == 5) return 10;
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
                return getBool(row.id) ? "ON" : "OFF";
            case 1:
                return modeLabel();
            case 3:
                return JumpResetMod.blatantKeep == 0
                        ? "ANTIKB"
                        : JumpResetMod.blatantKeep + "%";
            case 5:
                return JumpResetMod.antikbHold + "t";
            case 6:
                return JumpResetMod.chance + "%";
            case 7:
                return JumpResetMod.accuracy + "%";
            case 9:
                return JumpResetMod.velocityMissChance + "%";
            case 13:
                return JumpResetMod.delay + "t";
            default:
                return "";
        }
    }

    private String modeLabel() {
        if (JumpResetMod.mode == JumpResetMod.Mode.BLATANT) {
            return "Blatant";
        }
        int lo = (int) Math.round((JumpResetMod.mode.velocityFactor - JumpResetMod.mode.randomizeRange) * 100.0D);
        int hi = (int) Math.round((JumpResetMod.mode.velocityFactor + JumpResetMod.mode.randomizeRange) * 100.0D);
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

    // ---------------- render ----------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x90000000);
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + panelH, PANEL_BG);
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + 2, ACCENT);

        drawString(fontRendererObj, "Jump Reset", panelX + PAD, panelY + 10, 0xFFFFFFFF);

        String ver = "v" + JumpResetMod.VERSION;
        drawString(fontRendererObj, ver,
                panelX + PANEL_W - PAD - fontRendererObj.getStringWidth(ver),
                panelY + 10, TEXT_DIM);

        drawString(fontRendererObj, "Z / ESC fecha - salva ao fechar", panelX + PAD, panelY + 23, TEXT_DIM);

        for (Row row : rows) {
            drawRect(row.x, row.y, row.x + row.w, row.y + ROW_H - 3, ROW_BG);

            drawString(fontRendererObj, row.name, row.x + 6, row.y + 5, TEXT);

            String value = valueText(row);
            drawString(fontRendererObj, value,
                    row.x + row.w - 6 - fontRendererObj.getStringWidth(value),
                    row.y + 5, valueColor(row));

            if (row.kind == Kind.SLIDER) {
                int left = row.x + 6;
                int right = row.x + row.w - 6;
                int trackY = row.y + ROW_H - 7;

                drawRect(left, trackY, right, trackY + 2, TRACK_BG);

                float ratio = (float) getSlider(row.id) / (float) sliderMax(row.id);
                int fill = left + Math.round((right - left) * ratio);
                drawRect(left, trackY, fill, trackY + 2, ACCENT);
                drawRect(fill - 1, trackY - 2, fill + 2, trackY + 4, 0xFFFFFFFF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ---------------- input ----------------

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }

        for (Row row : rows) {
            if (mouseX < row.x || mouseX > row.x + row.w) {
                continue;
            }
            if (mouseY < row.y || mouseY >= row.y + ROW_H - 3) {
                continue;
            }

            if (row.kind == Kind.CHECK) {
                setBool(row.id, !getBool(row.id));
            } else if (row.kind == Kind.MODE) {
                JumpResetMod.mode = JumpResetMod.mode.next();
            } else {
                dragging = row;
                dragSlider(row, mouseX);
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (dragging != null) {
            dragSlider(dragging, mouseX);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_Z || keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void dragSlider(Row row, int mouseX) {
        int left = row.x + 6;
        int right = row.x + row.w - 6;
        float ratio = (float) (mouseX - left) / (float) (right - left);
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        setSlider(row.id, Math.round(ratio * sliderMax(row.id)));
    }

    @Override
    public void onGuiClosed() {
        JumpResetMod.saveConfig();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
