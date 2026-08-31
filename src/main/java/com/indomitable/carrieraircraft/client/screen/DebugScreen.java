package com.indomitable.carrieraircraft.client.screen;

import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.menu.DebugMenu;
import com.indomitable.carrieraircraft.network.DebugCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试工具 GUI 界面。
 *
 * <h2>布局（260 × 210）</h2>
 * <pre>
 *   0 ┌───────────────────────────────────┐
 *     │         飞机状态调试工具            │  标题
 *  14 ├───────────────────────────────────┤
 *     │ [<] B25 #1234 STANDBY [>]         │  飞机选择器
 *  30 ├───────────────────────────────────┤
 *     │ [状态按钮网格 3×3]                 │  状态选择
 *  82 ├───────────────────────────────────┤
 *     │ 起点 X[_] Y[_] Z[_]              │  坐标配置
 *     │ 目标 X[_] Y[_] Z[_]              │
 * 118 ├───────────────────────────────────┤
 *     │ [x 循环]  [执行] [停止]            │  控制
 * 138 ├───────────────────────────────────┤
 *     │ 运行状态: 就绪                      │  状态栏
 * 152 ├───────────────────────────────────┤
 *     │          [返回控制终端]             │
 * 170 ├───────────────────────────────────┤
 *     │          玩家背包                  │
 * 210 └───────────────────────────────────┘
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class DebugScreen extends AbstractContainerScreen<DebugMenu> {

    private static final int BG_COLOR     = 0xFF1A1A2E;
    private static final int HEADER_COLOR = 0xFF16213E;
    private static final int DIVIDER      = 0xFF0F3460;
    private static final int TEXT_COLOR    = 0xFFE0E0E0;
    private static final int DIM_TEXT      = 0xFF888888;
    private static final int ACTIVE_COLOR  = 0xFF50C4FF;
    private static final int SELECTED_BG   = 0xFF0F3460;

    private static final AircraftState[] ALL_STATES = AircraftState.values();
    /**
     * M7：使用 {@link List} 而非数组，配合 {@link #requireStateFlag} 越界检查，
     * 避免新增 {@link AircraftState} 后忘记同步数组而 NPE/AIOOBE。
     */
    private static final List<Boolean> NEEDS_TARGET = List.of(
            false, true, true, true, true, true, true, true, false);
    private static final List<Boolean> NEEDS_START = List.of(
            false, false, true, true, true, true, true, true, true);

    private int selectedAircraft = 0;
    private int selectedState = 0;

    private EditBox startX, startY, startZ;
    private EditBox targetX, targetY, targetZ;
    private Checkbox loopCheckbox;
    private boolean loopEnabled = false;

    private String runStatus = "就绪";

    public DebugScreen(DebugMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 260;
        this.imageHeight = 210;
    }

    @Override
    protected void init() {
        super.init();

        // ── 飞机选择器 ──
        int cx = leftPos + imageWidth / 2;
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> changeAircraft(-1))
                .pos(leftPos + 8, topPos + 14).size(20, 14).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> changeAircraft(1))
                .pos(leftPos + imageWidth - 28, topPos + 14).size(20, 14).build());

        // ── 状态按钮网格（3列 × 3行）──
        int gridX = leftPos + 8;
        int gridY = topPos + 32;
        int btnW = 80;
        int btnH = 14;
        int gap = 2;
        for (int i = 0; i < ALL_STATES.length; i++) {
            final int idx = i;
            int col = i % 3;
            int row = i / 3;
            int bx = gridX + col * (btnW + gap);
            int by = gridY + row * (btnH + gap);
            addRenderableWidget(Button.builder(
                    Component.translatable(ALL_STATES[i].translationKey()),
                    b -> selectState(idx)
            ).pos(bx, by).size(btnW, btnH).build());
        }

        // ── 坐标输入框 ──
        int fieldY1 = topPos + 86;
        int fieldY2 = topPos + 102;
        int labelX = leftPos + 8;
        int fieldX = labelX + 20;
        int fieldW = 55;

        startX = new EditBox(font, fieldX, fieldY1, fieldW, 12, Component.literal("startX"));
        startY = new EditBox(font, fieldX + fieldW + 8, fieldY1, fieldW, 12, Component.literal("startY"));
        startZ = new EditBox(font, fieldX + (fieldW + 8) * 2, fieldY1, fieldW, 12, Component.literal("startZ"));
        targetX = new EditBox(font, fieldX, fieldY2, fieldW, 12, Component.literal("targetX"));
        targetY = new EditBox(font, fieldX + fieldW + 8, fieldY2, fieldW, 12, Component.literal("targetY"));
        targetZ = new EditBox(font, fieldX + (fieldW + 8) * 2, fieldY2, fieldW, 12, Component.literal("targetZ"));

        startX.setHint(Component.literal("X"));
        startY.setHint(Component.literal("Y"));
        startZ.setHint(Component.literal("Z"));
        targetX.setHint(Component.literal("X"));
        targetY.setHint(Component.literal("Y"));
        targetZ.setHint(Component.literal("Z"));

        // 默认值
        startX.setValue("0"); startY.setValue("80"); startZ.setValue("0");
        targetX.setValue("0"); targetY.setValue("64"); targetZ.setValue("0");

        addRenderableWidget(startX);
        addRenderableWidget(startY);
        addRenderableWidget(startZ);
        addRenderableWidget(targetX);
        addRenderableWidget(targetY);
        addRenderableWidget(targetZ);

        // ── 循环开关 ──
        // 用普通按钮模拟复选框
        addRenderableWidget(Button.builder(
                Component.literal("☐ 循环"),
                b -> {
                    loopEnabled = !loopEnabled;
                    b.setMessage(Component.literal(loopEnabled ? "☑ 循环" : "☐ 循环"));
                }
        ).pos(leftPos + 8, topPos + 118).size(60, 14).build());

        // ── 执行 / 停止 ──
        addRenderableWidget(Button.builder(Component.literal("▶ 执行"), b -> executeDebug())
                .pos(leftPos + 76, topPos + 118).size(60, 14).build());
        addRenderableWidget(Button.builder(Component.literal("■ 停止"), b -> stopDebug())
                .pos(leftPos + 140, topPos + 118).size(60, 14).build());

        // ── 返回按钮 ──
        addRenderableWidget(Button.builder(Component.literal("返回控制终端"), b -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        }).pos(leftPos + imageWidth / 2 - 50, topPos + 152).size(100, 14).build());
    }

    private void changeAircraft(int delta) {
        List<DebugMenu.AircraftInfo> list = menu.aircraftList();
        if (list.isEmpty()) return;
        selectedAircraft = (selectedAircraft + delta + list.size()) % list.size();
    }

    private void selectState(int index) {
        selectedState = index;
    }

    private void executeDebug() {
        List<DebugMenu.AircraftInfo> list = menu.aircraftList();
        if (list.isEmpty()) return;

        double sx = parseDouble(startX.getValue(), 0);
        double sy = parseDouble(startY.getValue(), 80);
        double sz = parseDouble(startZ.getValue(), 0);
        double tx = parseDouble(targetX.getValue(), 0);
        double ty = parseDouble(targetY.getValue(), 64);
        double tz = parseDouble(targetZ.getValue(), 0);

        DebugCommandPayload pkt = new DebugCommandPayload(
                DebugCommandPayload.EXECUTE,
                selectedAircraft,
                (byte) selectedState,
                sx, sy, sz,
                tx, ty, tz,
                loopEnabled
        );
        PacketDistributor.sendToServer(pkt);
        runStatus = "执行中 " + Component.translatable(ALL_STATES[selectedState].translationKey()).getString()
                + (loopEnabled ? " [循环]" : "");
    }

    private void stopDebug() {
        DebugCommandPayload pkt = new DebugCommandPayload(
                DebugCommandPayload.STOP,
                0, (byte) 0,
                0, 0, 0, 0, 0, 0,
                false
        );
        PacketDistributor.sendToServer(pkt);
        runStatus = "已停止";
    }

    private static double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return fallback; }
    }

    // ── 渲染 ──

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG_COLOR);

        // 分隔线
        g.fill(leftPos, topPos + 12, leftPos + imageWidth, topPos + 14, HEADER_COLOR);
        g.fill(leftPos, topPos + 30, leftPos + imageWidth, topPos + 32, DIVIDER);
        g.fill(leftPos, topPos + 84, leftPos + imageWidth, topPos + 86, DIVIDER);
        g.fill(leftPos, topPos + 116, leftPos + imageWidth, topPos + 118, DIVIDER);
        g.fill(leftPos, topPos + 136, leftPos + imageWidth, topPos + 138, DIVIDER);
        g.fill(leftPos, topPos + 150, leftPos + imageWidth, topPos + 152, DIVIDER);
        g.fill(leftPos, topPos + 170, leftPos + imageWidth, topPos + 172, DIVIDER);
    }

    private static boolean requireStateFlag(List<Boolean> flags, int index) {
        if (index < 0 || index >= flags.size()) return false;
        Boolean v = flags.get(index);
        return v != null && v;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // 标题
        g.drawString(font, "飞机状态调试工具", (imageWidth - font.width("飞机状态调试工具")) / 2, 2, ACTIVE_COLOR, false);

        // 飞机信息
        List<DebugMenu.AircraftInfo> list = menu.aircraftList();
        if (list.isEmpty()) {
            g.drawString(font, "无在役飞机", leftPos + 32 - leftPos, 17, DIM_TEXT, false);
        } else {
            DebugMenu.AircraftInfo info = list.get(selectedAircraft);
            String text = String.format("%s #%s %s",
                    info.role().displayName(),
                    info.uuid().toString().substring(0, 4),
                    Component.translatable(info.state().translationKey()).getString());
            int tw = font.width(text);
            g.drawString(font, text, (imageWidth - tw) / 2, 17, TEXT_COLOR, false);
        }

        // 状态选择高亮
        for (int i = 0; i < ALL_STATES.length; i++) {
            if (i == selectedState) {
                int col = i % 3;
                int row = i / 3;
                int bx = 8 + col * 82;
                int by = 32 + row * 16 - 2; // 微调对齐
                g.fill(bx - 1, topPos + 32 + row * 16 - 1 - topPos,
                        bx + 81, topPos + 32 + row * 16 + 15 - topPos,
                        0x4050C4FF);
            }
        }

        // 坐标标签
        boolean showStart = requireStateFlag(NEEDS_START, selectedState);
        boolean showTarget = requireStateFlag(NEEDS_TARGET, selectedState);

        if (showStart) {
            g.drawString(font, "起点", 8, 89, TEXT_COLOR, false);
        }
        if (showTarget) {
            g.drawString(font, "目标", 8, 105, TEXT_COLOR, false);
        }
        if (!showStart && !showTarget) {
            g.drawString(font, "该状态无需额外配置", 8, 92, DIM_TEXT, false);
        }

        // 运行状态
        g.drawString(font, "状态: " + runStatus, 8, 140, 0xFF90FF90, false);

        // 状态按钮下方的选中指示
        if (selectedState >= 0 && selectedState < ALL_STATES.length) {
            int col = selectedState % 3;
            int row = selectedState / 3;
            int bx = leftPos + 8 + col * 82;
            int by = topPos + 32 + row * 16;
            g.fill(bx, by, bx + 80, by + 14, 0x4050C4FF);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    // 控制坐标输入框的可见性
    @Override
    public void containerTick() {
        super.containerTick();
        boolean showStart = requireStateFlag(NEEDS_START, selectedState);
        boolean showTarget = requireStateFlag(NEEDS_TARGET, selectedState);
        startX.visible = showStart;
        startY.visible = showStart;
        startZ.visible = showStart;
        targetX.visible = showTarget;
        targetY.visible = showTarget;
        targetZ.visible = showTarget;
    }
}
