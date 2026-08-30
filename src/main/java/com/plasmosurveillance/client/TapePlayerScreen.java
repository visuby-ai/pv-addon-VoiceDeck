package com.plasmosurveillance.client;

import com.plasmosurveillance.item.ModItems;
import com.plasmosurveillance.item.TapePlayerItem;
import com.plasmosurveillance.network.NetworkHandler;
import com.plasmosurveillance.network.TapePlayerActionPacket;
import com.plasmosurveillance.tape.TapeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Fully custom tape-player UI. No vanilla buttons and no frame texture. */
public class TapePlayerScreen extends Screen {
    private final InteractionHand hand;
    private final List<ItemStack> tapes = new ArrayList<>();
    private int index = 0;
    private int range = 16;
    private GLFWDropCallback oldDropCallback;

    private static final int W = 340;
    private static final int H = 240;

    // Palette: dark surveillance console / cassette recorder.
    private static final int OUTER = 0xFF202B2A;
    private static final int OUTER_EDGE = 0xFF506361;
    private static final int INNER = 0xFF0D211D;
    private static final int PANEL = 0xFF16352D;
    private static final int PANEL_2 = 0xFF1D4338;
    private static final int LINE = 0xFF86C8A4;
    private static final int ACCENT = 0xFFA8EE76;
    private static final int TEXT = 0xFFE7F0EA;
    private static final int MUTED = 0xFF9EB6AB;
    private static final int DARK = 0xFF0A1512;

    public TapePlayerScreen(InteractionHand hand) {
        super(Component.empty());
        this.hand = hand;
    }

    @Override
    protected void init() {
        refreshTapes();
        ItemStack stack = menuStack();
        if (stack.getTag() != null && stack.getTag().contains(TapePlayerItem.RANGE)) {
            range = Math.max(1, Math.min(64, stack.getTag().getInt(TapePlayerItem.RANGE)));
        }
        oldDropCallback = GLFW.glfwSetDropCallback(
                Minecraft.getInstance().getWindow().getWindow(),
                GLFWDropCallback.create((window, count, names) -> {
                    if (count > 0) {
                        Minecraft.getInstance().execute(() ->
                                AudioImportManager.handleDrop(GLFWDropCallback.getName(names, 0)));
                    }
                })
        );
    }

    private ItemStack menuStack() { return minecraft.player.getItemInHand(hand); }

    private void refreshTapes() {
        tapes.clear();
        for (ItemStack s : minecraft.player.getInventory().items) {
            if (s.is(ModItems.RECORDED_TAPE.get()) && TapeData.readFromStack(s) != null) tapes.add(s);
        }
        UUID selected = menuStack().getTag() != null && menuStack().getTag().hasUUID(TapePlayerItem.SELECTED_TAPE)
                ? menuStack().getTag().getUUID(TapePlayerItem.SELECTED_TAPE) : null;
        if (selected != null) {
            for (int i = 0; i < tapes.size(); i++) {
                TapeData d = TapeData.readFromStack(tapes.get(i));
                if (d != null && selected.equals(d.tapeId)) { index = i; break; }
            }
        }
        if (index >= tapes.size()) index = 0;
    }

    private UUID currentId() { return tapes.isEmpty() ? null : TapeData.readFromStack(tapes.get(index)).tapeId; }

    private void sendSelect() {
        if (currentId() != null)
            NetworkHandler.CHANNEL.sendToServer(new TapePlayerActionPacket(TapePlayerActionPacket.SELECT, hand, currentId(), range));
    }
    private void sendRange() {
        NetworkHandler.CHANNEL.sendToServer(new TapePlayerActionPacket(TapePlayerActionPacket.RANGE, hand, null, range));
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }
    private boolean hit(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public void tick() { super.tick(); refreshTapes(); }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int l = left(), t = top();
        if (button == 0) {
            if (hit(l + 24, t + 102, 50, 28, mx, my)) {
                if (!tapes.isEmpty()) { index = (index - 1 + tapes.size()) % tapes.size(); sendSelect(); }
                return true;
            }
            if (hit(l + 266, t + 102, 50, 28, mx, my)) {
                if (!tapes.isEmpty()) { index = (index + 1) % tapes.size(); sendSelect(); }
                return true;
            }
            if (hit(l + 124, t + 102, 42, 28, mx, my)) { range = Math.max(1, range - 5); sendRange(); return true; }
            if (hit(l + 174, t + 102, 42, 28, mx, my)) { range = Math.min(64, range + 5); sendRange(); return true; }
            if (hit(l + 24, t + 174, 292, 28, mx, my)) { AudioImportManager.chooseFile(); return true; }
            if (hit(l + 24, t + 206, 292, 28, mx, my)) {
                NetworkHandler.CHANNEL.sendToServer(new TapePlayerActionPacket(TapePlayerActionPacket.PLAY, hand, currentId(), range));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int l = left(), t = top(), cx = l + W / 2;

        // Entire console is rendered here. The old frame texture is intentionally not used.
        g.fill(l, t, l + W, t + H, OUTER);
        g.fill(l + 4, t + 4, l + W - 4, t + H - 4, OUTER_EDGE);
        g.fill(l + 8, t + 8, l + W - 8, t + H - 8, INNER);
        g.fill(l + 12, t + 12, l + W - 12, t + H - 12, DARK);

        // Corner screws / rivets.
        screw(g, l + 18, t + 18); screw(g, l + W - 18, t + 18);
        screw(g, l + 18, t + H - 18); screw(g, l + W - 18, t + H - 18);

        // Header: small machine label, not a generic title.
        g.drawString(font, "PLASMO SURVEILLANCE", l + 28, t + 22, MUTED);
        g.drawString(font, "TAPE DECK", l + W - 88, t + 22, ACCENT);

        // Cassette display.
        int px = l + 24, py = t + 38, pw = W - 48, ph = 54;
        g.fill(px, py, px + pw, py + ph, PANEL);
        g.fill(px + 2, py + 2, px + pw - 2, py + 5, LINE);
        g.fill(px + 2, py + ph - 5, px + pw - 2, py + ph - 2, LINE);
        g.fill(px + 12, py + 11, px + 48, py + 43, PANEL_2);
        g.fill(px + pw - 48, py + 11, px + pw - 12, py + 43, PANEL_2);
        reel(g, px + 30, py + 27); reel(g, px + pw - 30, py + 27);

        String name = "NO TAPE";
        String detail = "DROP / IMPORT A WAV";
        if (!tapes.isEmpty()) {
            TapeData d = TapeData.readFromStack(tapes.get(index));
            name = tapes.get(index).hasCustomHoverName() ? tapes.get(index).getHoverName().getString() : "TAPE " + (index + 1);
            detail = (index + 1) + " / " + tapes.size() + "   •   " + (d != null ? format(d.lengthSeconds) : "--:--");
        }
        g.drawCenteredString(font, name, cx, py + 9, ACCENT);
        g.drawCenteredString(font, detail, cx, py + 32, TEXT);

        // Range line.
        g.drawString(font, "RANGE", l + 24, t + 84, MUTED);
        g.drawCenteredString(font, range + " BLOCKS", cx, t + 84, TEXT);
        g.drawString(font, "1", l + W - 42, t + 84, MUTED);
        g.drawString(font, "64", l + W - 26, t + 84, MUTED);

        drawButton(g, l + 24, t + 102, 50, 28, mouseX, mouseY, "", Icon.LEFT);
        drawButton(g, l + 266, t + 102, 50, 28, mouseX, mouseY, "", Icon.RIGHT);
        drawButton(g, l + 124, t + 102, 42, 28, mouseX, mouseY, "", Icon.MINUS);
        drawButton(g, l + 174, t + 102, 42, 28, mouseX, mouseY, "", Icon.PLUS);

        // Import area.
        g.fill(l + 24, t + 136, l + W - 24, t + 168, PANEL);
        String status = AudioImportManager.isImporting() ? "IMPORTING" : AudioImportManager.getStatus();
        if (status == null || status.isEmpty()) status = "DROP WAV TO IMPORT";
        if (status.length() > 30) status = status.substring(0, 30) + "...";
        g.drawCenteredString(font, status, cx, t + 140, AudioImportManager.isImporting() ? ACCENT : MUTED);
        long total = AudioImportManager.getTotalBytes(), sent = AudioImportManager.getSentBytes();
        int barW = 230;
        g.fill(cx - barW / 2, t + 153, cx + barW / 2, t + 157, DARK);
        int filled = total > 0 ? (int)(barW * Math.min(1d, (double)sent / total)) : 0;
        if (filled > 0) g.fill(cx - barW / 2, t + 153, cx - barW / 2 + filled, t + 157, LINE);
        g.drawCenteredString(font, AudioImportManager.isImporting() ? "SENDING AUDIO" : "READY", cx, t + 160, MUTED);

        drawButton(g, l + 24, t + 174, 292, 28, mouseX, mouseY, "IMPORT WAV", Icon.IMPORT);
        drawButton(g, l + 24, t + 206, 292, 28, mouseX, mouseY, "PLAY", Icon.PLAY);
    }

    private enum Icon { LEFT, RIGHT, MINUS, PLUS, IMPORT, PLAY }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, double mx, double my, String label, Icon icon) {
        boolean hover = hit(x, y, w, h, mx, my);
        int edge = hover ? ACCENT : OUTER_EDGE;
        int fill = hover ? 0xFF2A5145 : 0xFF263B3B;
        g.fill(x, y, x + w, y + h, edge);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
        g.fill(x + 3, y + 3, x + w - 3, y + 5, hover ? 0xFFB8F28D : 0xFF64827B);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 2, DARK);
        int c = hover ? 0xFFF3FFF5 : TEXT;
        if (icon == Icon.LEFT || icon == Icon.RIGHT) arrow(g, x + w / 2, y + h / 2, icon == Icon.RIGHT, c);
        else if (icon == Icon.MINUS || icon == Icon.PLUS) math(g, x + w / 2, y + h / 2, icon == Icon.PLUS, c);
        else if (icon == Icon.IMPORT) { importIcon(g, x + 28, y + h / 2, c); g.drawCenteredString(font, label, x + w / 2 + 12, y + 8, c); }
        else if (icon == Icon.PLAY) { playIcon(g, x + 28, y + h / 2, c); g.drawCenteredString(font, label, x + w / 2 + 12, y + 8, c); }
    }

    private void arrow(GuiGraphics g, int cx, int cy, boolean right, int c) {
        if (right) { g.fill(cx - 7, cy - 2, cx + 2, cy + 3, c); g.fill(cx + 1, cy - 6, cx + 5, cy + 7, c); g.fill(cx + 4, cy - 4, cx + 8, cy + 5, c); }
        else { g.fill(cx - 2, cy - 2, cx + 7, cy + 3, c); g.fill(cx - 5, cy - 6, cx - 1, cy + 7, c); g.fill(cx - 8, cy - 4, cx - 4, cy + 5, c); }
    }
    private void math(GuiGraphics g, int cx, int cy, boolean plus, int c) {
        g.fill(cx - 7, cy - 2, cx + 8, cy + 3, c); if (plus) g.fill(cx - 2, cy - 7, cx + 3, cy + 8, c);
    }
    private void playIcon(GuiGraphics g, int cx, int cy, int c) {
        g.fill(cx - 6, cy - 8, cx - 1, cy + 9, c); g.fill(cx - 1, cy - 6, cx + 3, cy + 7, c); g.fill(cx + 3, cy - 4, cx + 7, cy + 5, c); g.fill(cx + 7, cy - 2, cx + 9, cy + 3, c);
    }
    private void importIcon(GuiGraphics g, int cx, int cy, int c) {
        g.fill(cx - 9, cy + 6, cx + 10, cy + 9, c); g.fill(cx - 2, cy - 9, cx + 3, cy + 5, c); g.fill(cx - 7, cy - 5, cx + 8, cy - 1, c); g.fill(cx - 6, cy - 7, cx - 1, cy - 4, c); g.fill(cx + 2, cy - 7, cx + 7, cy - 4, c);
    }
    private void screw(GuiGraphics g, int x, int y) { g.fill(x - 2, y - 2, x + 3, y + 3, 0xFF73817E); g.fill(x - 1, y, x + 2, y + 1, DARK); }
    private void reel(GuiGraphics g, int cx, int cy) { g.fill(cx - 13, cy - 13, cx + 14, cy + 14, 0xFF8CCDA7); g.fill(cx - 9, cy - 9, cx + 10, cy + 10, PANEL); g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF8CCDA7); }
    private String format(int s) { return String.format("%d:%02d", s / 60, s % 60); }

    @Override public void onClose() {
        if (oldDropCallback != null) {
            GLFW.glfwSetDropCallback(Minecraft.getInstance().getWindow().getWindow(), oldDropCallback);
            oldDropCallback = null;
        }
        super.onClose();
    }
}
