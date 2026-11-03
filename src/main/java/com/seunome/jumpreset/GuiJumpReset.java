package com.seunome.jumpreset;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiJumpReset extends GuiScreen {

    private enum Kind { CHECK, MODE, SLIDER }

    private static final int PANEL_W = 300;
    private static final int ROW_H = 22;
    private static final int PAD = 10;

    private static final int ACCENT = 0xFF8E6BF0;
    private static final int ACCENT_DIM = 0xFFB9A8FF;
    private static final int PANEL_BG = 0xF0101016;
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
        final int y;

        Row(int id, String name, Kind kind, int y) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.y = y;
        }
    }

    @Override
    public void initGui() {
        rows.clear();
        panelX = (width - PANEL_W) / 2;
        panelH = 52 + 11 * ROW_H + 22;
        panelY = (height - panelH) / 2;

        int y = panelY + 46;
        rows.add(new Row(0, "Jump Reset", Kind.CHECK, y));          y += ROW_H;
        rows.add(new Row(1, "Mode", Kind.MODE, y));                 y += ROW_H;
        rows.add(new Row(2, "Velocity", Kind.CHECK, y));            y += ROW_H;
        rows.add(new Row(10, "Blatant KB", Kind.SLIDER, y));        y += ROW_H;
        rows.add(new Row(8, "Randomize", Kind.CHECK, y));           y += ROW_H;
        rows.add(new Row(9, "Miss Chance", Kind.SLIDER, y));        y += ROW_H;
        rows.add(new Row(3, "Chance", Kind.SLIDER, y));             y += ROW_H;
        rows.add(new Row(4, "Accuracy", Kind.SLIDER, y));           y += ROW_H;
        rows.add(new Row(5, "Only When Targeting", Kind.CHECK, y)); y += ROW_H;
        rows.add(new Row(6, "Water Check", Kind.CHECK, y));         y += ROW_H;
        rows.add(new Row(7, "Delay", Kind.SLIDER, y));
    }

    // ---------------- valores ----------------

    private boolean getBool(int id) {
        switch (id) {
            case 0: return JumpResetMod.enabled;
            case 2: return JumpResetMod.velocityEnabled;
            case 8: return JumpResetMod.velocityRandomize;
            case 5: return JumpResetMod.onlyWhenTargeting;
            case 6: return JumpResetMod.waterCheck;
            default: return false;
        }
    }

    private void setBool(int id, boolean value) {
        switch (id) {
            case 0: JumpResetMod.enabled = value; break;
            case 2: JumpResetMod.velocityEnabled = value; break;
            case 8: JumpResetMod.velocityRandomize = value; break;
            case 5: JumpResetMod.onlyWhenTargeting = value; break;
            case 6: JumpResetMod.waterCheck = value; break;
        }
    }

    private int getSlider(int id) {
        switch (id) {
            case 3: return JumpResetMod.chance;
            case 4: return JumpResetMod.accuracy;
            case 7: return JumpResetMod.delay;
            case 9: return JumpResetMod.velocityMissChance;
            case 10: return JumpResetMod.blatantKeep;
            default: return 0;
        }
    }

    private void setSlider(int id, int value) {
        switch (id) {
            case 3: JumpResetMod.chance = value; break;
            case 4: JumpResetMod.accuracy = value; break;
            case 7: JumpResetMod.delay = value; break;
            case 9: JumpResetMod.velocityMissChance = value; break;
            case 10: JumpResetMod.blatantKeep = value; break;
        }
    }

    private int sliderMax(int id) {
        return id == 7 ? 5 : 100;
    }

    private String valueText(Row row) {
        switch (row.id) {
            case 0:
            case 2:
            case 5:
            case 6:
            case 8:
                return getBool(row.id) ? "ON" : "OFF";
            case 1:
                return modeLabel();
            case 3:
                return JumpResetMod.chance + "%";
            case 4:
                return JumpResetMod.accuracy + "%";
            case 7:
                return JumpResetMod.delay + " tick" + (JumpResetMod.delay == 1 ? "" : "s");
            case 9:
                return JumpResetMod.velocityMissChance + "%";
            case 10:
                return JumpResetMod.blatantKeep + "%";
            default:
                return "";
        }
    }

    private String modeLabel() {
        if (JumpResetMod.mode == JumpResetMod.Mode.BLATANT) {
            return "Blatant  " + JumpResetMod.blatantKeep + "%";
        }
        double r = JumpResetMod.mode.randomizeRange * 100.0D;
        int lo = (int) Math.round((JumpResetMod.mode.velocityFactor - JumpResetMod.mode.randomizeRange) * 100.0D);
        int hi = (int) Math.round((JumpResetMod.mode.velocityFactor + JumpResetMod.mode.randomizeRange) * 100.0D);
        return JumpResetMod.mode.label + "  " + lo + "-" + hi + "%";
    }

    private int valueColor(Row row) {
        if (row.kind == Kind.CHECK) {
            return getBool(row.id) ? GREEN : RED;
        }
        return ACCENT_DIM;
    }

    // ---------------- render ----------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x80000000);
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + panelH, PANEL_BG);
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + 2, ACCENT);

        drawString(fontRendererObj, "Jump Reset", panelX + PAD, panelY + 12, 0xFFFFFFFF);
        int nameW = fontRendererObj.getStringWidth("Jump Reset");
        drawString(fontRendererObj, "v" + JumpResetMod.VERSION, panelX + PAD + nameW + 6, panelY + 12, TEXT_DIM);
        drawString(fontRendererObj, "Z / ESC para fechar", panelX + PAD, panelY + 26, TEXT_DIM);

        for (Row row : rows) {
            drawRect(panelX + 4, row.y, panelX + PANEL_W - 4, row.y + ROW_H - 3, ROW_BG);

            drawString(fontRendererObj, row.name, panelX + PAD, row.y + 5, TEXT);

            String value = valueText(row);
            drawString(fontRendererObj, value,
                    panelX + PANEL_W - PAD - fontRendererObj.getStringWidth(value),
                    row.y + 5, valueColor(row));

            if (row.kind == Kind.SLIDER) {
                int trackLeft = panelX + PAD;
                int trackRight = panelX + PANEL_W - PAD;
                int trackY = row.y + ROW_H - 7;
                int trackW = trackRight - trackLeft;

                drawRect(trackLeft, trackY, trackRight, trackY + 2, TRACK_BG);

                float ratio = (float) getSlider(row.id) / (float) sliderMax(row.id);
                int fill = trackLeft + Math.round(trackW * ratio);
                drawRect(trackLeft, trackY, fill, trackY + 2, ACCENT);
                drawRect(fill - 1, trackY - 2, fill + 2, trackY + 4, 0xFFFFFFFF);
            }
        }

        drawCenteredString(fontRendererObj, "Clique nos valores / arraste os sliders - salva ao fechar",
                width / 2, panelY + panelH - 14, TEXT_DIM);

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
            if (mouseY < row.y || mouseY >= row.y + ROW_H - 3) {
                continue;
            }
            if (mouseX < panelX + 4 || mouseX > panelX + PANEL_W - 4) {
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
        int left = panelX + PAD;
        int right = panelX + PANEL_W - PAD;
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
