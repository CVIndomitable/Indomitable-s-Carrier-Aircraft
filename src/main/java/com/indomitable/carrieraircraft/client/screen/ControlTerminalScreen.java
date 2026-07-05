package com.indomitable.carrieraircraft.client.screen;

import com.indomitable.carrieraircraft.entity.ai.AircraftState;
import com.indomitable.carrieraircraft.menu.ControlTerminalMenu;
import com.indomitable.carrieraircraft.network.AddTargetPayload;
import com.indomitable.carrieraircraft.network.CommandPayload;
import com.indomitable.carrieraircraft.network.FormationCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 控制终端 GUI 界面 —— 三栏布局 + 小地图。
 *
 * <h2>主界面布局（272 × 220）</h2>
 * <pre>
 *   0 ┌──────────────────────────────────────────┐
 *     │              舰载机控制终端                 │  标题
 *  12 ├────────────┬──────────────┬───────────────┤
 *     │            │              │  X [____]     │
 *     │  飞机列表   │   小地图      │  Y [____]     │
 *     │  (左栏)    │  (中栏)      │  Z [____]     │
 *     │            │              │  [添加打击目标] │
 *     │            │              │  ● 盘旋点      │
 * 184 ├────────────┴──────────────┼───────────────┤
 *     │           玩家背包 3×9+9   │[设置][编队][盘旋][召回]
 * 200 ├───────────────────────────┤               │
 *     │           快捷栏           │               │
 * 220 └───────────────────────────┴───────────────┘
 * </pre>
 *
 * <h2>子页面</h2>
 * <p>点击"设置"/"编队"按钮切换到对应子页面，覆盖中间和右侧区域。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ControlTerminalScreen extends AbstractContainerScreen<ControlTerminalMenu> {

    // ── 颜色常量 ──
    private static final int BG_COLOR       = 0xFF1A1A2E;
    private static final int HEADER_COLOR   = 0xFF16213E;
    private static final int DIVIDER        = 0xFF0F3460;
    private static final int PANEL_BG       = 0xFF12122A;
    private static final int TEXT_COLOR      = 0xFFE0E0E0;
    private static final int DIM_TEXT        = 0xFF888888;
    private static final int MAP_BG         = 0xFF0A0A1A;
    private static final int MAP_GRID       = 0xFF1A1A3A;
    private static final int MAP_PLAYER     = 0xFFFFFFFF;
    private static final int MAP_RALLY      = 0xFF50C4FF;
    private static final int MAP_TARGET     = 0xFFFF5555;
    private static final int MAP_TARGET_ENT = 0xFFFF9800;
    private static final int DROPDOWN_BG    = 0xFF16213E;
    private static final int DROPDOWN_HOVER = 0xFF0F3460;
    private static final int DROPDOWN_SEL   = 0xFF1A3A6E;
    private static final int DROPDOWN_BORDER= 0xFF0F3460;

    // ── 布局常量 ──
    private static final int WIDTH  = 272;
    private static final int HEIGHT = 220;

    // 左栏：飞机列表
    private static final int LEFT_X = 6;
    private static final int LEFT_W = 76;

    // 中栏：小地图
    private static final int MAP_X = LEFT_X + LEFT_W + 4; // 86
    private static final int MAP_W = 92;
    private static final int MAP_H = 108;
    private static final int MAP_Y = 14;

    // 右栏
    private static final int RIGHT_X = MAP_X + MAP_W + 4; // 182
    private static final int RIGHT_W = 84;

    // 底部按钮区
    private static final int BTN_AREA_Y = 184;

    // 小地图：每像素代表的世界距离
    private static final double MAP_SCALE = 2.5; // 100px = 250 格

    // ── 设置子页面常量 ──
    private static final String[] AUTO_LOCK_NAMES   = {"最近", "最强", "集火", "分散", "类型"};
    private static final String[] ASSIGN_NAMES      = {"集火", "均衡"};
    private static final String[] AIR_DEFENSE_NAMES = {"自卫", "主动", "管控"};
    private static final String[] BOMBS_NAMES       = {"1", "2", "3", "4"};
    private static final String[] MIN_DMG_NAMES     = {"0", "20", "40", "80"};

    private static class DropdownDef {
        final String[] options;
        final byte[] commands;
        final int y;
        final int currentGetter;

        DropdownDef(String[] options, byte[] commands, int y, int currentGetter) {
            this.options = options;
            this.commands = commands;
            this.y = y;
            this.currentGetter = currentGetter;
        }
    }

    private final DropdownDef[] dropdowns = {
        new DropdownDef(AUTO_LOCK_NAMES,
                new byte[]{CommandPayload.SET_AUTO_LOCK_NEAREST, CommandPayload.SET_AUTO_LOCK_STRONGEST,
                           CommandPayload.SET_AUTO_LOCK_FOCUS, CommandPayload.SET_AUTO_LOCK_SPREAD,
                           CommandPayload.SET_AUTO_LOCK_TYPE_FILTER},
                28, 0),
        new DropdownDef(ASSIGN_NAMES,
                new byte[]{CommandPayload.SET_ASSIGN_FOCUS, CommandPayload.SET_ASSIGN_SPREAD},
                44, 1),
        new DropdownDef(AIR_DEFENSE_NAMES,
                new byte[]{CommandPayload.SET_AIR_DEFENSE_SELF, CommandPayload.SET_AIR_DEFENSE_ACTIVE,
                           CommandPayload.SET_AIR_DEFENSE_LOW},
                60, 2),
        new DropdownDef(BOMBS_NAMES,
                new byte[]{CommandPayload.SET_BOMBS_1, CommandPayload.SET_BOMBS_2,
                           CommandPayload.SET_BOMBS_3, CommandPayload.SET_BOMBS_4},
                76, 3),
        new DropdownDef(MIN_DMG_NAMES,
                new byte[]{CommandPayload.SET_MIN_DMG_0, CommandPayload.SET_MIN_DMG_20,
                           CommandPayload.SET_MIN_DMG_40, CommandPayload.SET_MIN_DMG_80},
                92, 4),
    };

    // ── 状态 ──
    private boolean showSettings = false;
    private boolean showFormation = false;
    private int openDropdown = -1;
    private int openFormationDropdown = -1;

    private Button btnSettings, btnRally, btnRecall, btnFormation;
    private Button btnBack;            // 设置子页面的返回按钮
    private Button btnFormationBack;   // 编组子页面的返回按钮
    private Button btnAddTarget;       // 添加打击目标

    // 坐标输入框
    private EditBox inputX, inputY, inputZ;

    public ControlTerminalScreen(ControlTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    // ── 初始化 ──

    @Override
    protected void init() {
        super.init();

        // ── 坐标输入框（右栏）──
        int inputX0 = leftPos + RIGHT_X + 18;
        int inputW = RIGHT_W - 20;
        inputX = new EditBox(font, inputX0, topPos + MAP_Y + 16, inputW, 12, Component.literal("X"));
        inputY = new EditBox(font, inputX0, topPos + MAP_Y + 32, inputW, 12, Component.literal("Y"));
        inputZ = new EditBox(font, inputX0, topPos + MAP_Y + 48, inputW, 12, Component.literal("Z"));
        configureCoordField(inputX, "X");
        configureCoordField(inputY, "Y");
        configureCoordField(inputZ, "Z");

        // ── 添加打击目标按钮 ──
        btnAddTarget = Button.builder(Component.literal("添加打击目标"), b -> {
            blurCoordFields();
            onAddTarget();
        }).pos(leftPos + RIGHT_X + 2, topPos + MAP_Y + 64).size(RIGHT_W - 4, 14).build();

        // ── 底部按钮区（单行 4 个按钮）──
        int bx = leftPos + RIGHT_X;
        int by = topPos + BTN_AREA_Y;
        int quarterW = RIGHT_W / 4;

        btnSettings = Button.builder(Component.literal("设置"), b -> {
            blurCoordFields();
            switchToSettings();
        }).pos(bx, by).size(quarterW, 14).build();

        btnFormation = Button.builder(Component.literal("编队"), b -> {
            blurCoordFields();
            switchToFormation();
        }).pos(bx + quarterW, by).size(quarterW, 14).build();

        btnRally = Button.builder(Component.literal("盘旋"), b -> {
            blurCoordFields();
            sendCommand(CommandPayload.SET_RALLY_POINT);
        }).pos(bx + quarterW * 2, by).size(quarterW, 14).build();

        btnRecall = Button.builder(Component.literal("召回"), b -> {
            blurCoordFields();
            sendCommand(CommandPayload.RECALL_ALL);
        }).pos(bx + quarterW * 3, by).size(quarterW, 14).build();

        addRenderableWidget(btnSettings);
        addRenderableWidget(btnFormation);
        addRenderableWidget(btnRally);
        addRenderableWidget(btnRecall);
        addRenderableWidget(btnAddTarget);

        // ── 设置子页面返回按钮 ──
        btnBack = Button.builder(Component.literal("← 返回"), b -> switchToMain())
                .pos(leftPos + 8, topPos + 160).size(60, 14).build();
        addRenderableWidget(btnBack);

        // ── 编组子页面返回按钮 ──
        btnFormationBack = Button.builder(Component.literal("← 返回"), b -> switchToMain())
                .pos(leftPos + 8, topPos + 160).size(60, 14).build();
        addRenderableWidget(btnFormationBack);
    }

    private void configureCoordField(EditBox field, String placeholder) {
        field.setMaxLength(16);
        field.setEditable(true);
        field.setCanLoseFocus(false);
        field.setHint(Component.literal(placeholder));
        field.setTextColor(0xFF90FF90);
        field.setTextColorUneditable(DIM_TEXT);
        field.setFilter(s -> s.matches("[0-9.\\-]*"));
        addRenderableWidget(field);
    }

    private void blurCoordFields() {
        if (inputX != null) inputX.setFocused(false);
        if (inputY != null) inputY.setFocused(false);
        if (inputZ != null) inputZ.setFocused(false);
    }

    private void switchToSettings() {
        showSettings = true;
        showFormation = false;
        openDropdown = -1;
        openFormationDropdown = -1;
        updateButtonVisibility();
    }

    private void switchToFormation() {
        showFormation = true;
        showSettings = false;
        openDropdown = -1;
        openFormationDropdown = -1;
        updateButtonVisibility();
    }

    private void switchToMain() {
        showSettings = false;
        showFormation = false;
        openDropdown = -1;
        openFormationDropdown = -1;
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean main = !showSettings && !showFormation;
        btnSettings.visible = main;
        btnFormation.visible = main;
        btnRally.visible = main;
        btnRecall.visible = main;
        btnAddTarget.visible = main;
        inputX.visible = main;
        inputY.visible = main;
        inputZ.visible = main;
        btnBack.visible = showSettings;
        btnFormationBack.visible = showFormation;
    }

    // ── 鼠标点击 ──

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (showFormation) {
            return mouseClickedFormation(mouseX, mouseY, button);
        }
        if (showSettings) {
            return mouseClickedSettings(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean mouseClickedSettings(double mouseX, double mouseY, int button) {
        if (openDropdown >= 0) {
            DropdownDef dd = dropdowns[openDropdown];
            int px = leftPos + 80;
            int py = topPos + dd.y + 14;
            int rowH = 14;

            for (int i = 0; i < dd.options.length; i++) {
                int ry = py + i * rowH;
                if (mouseX >= px && mouseX < px + 160 && mouseY >= ry && mouseY < ry + rowH) {
                    sendCommand(dd.commands[i]);
                    openDropdown = -1;
                    return true;
                }
            }
            openDropdown = -1;
            return true;
        }

        for (int d = 0; d < dropdowns.length; d++) {
            DropdownDef dd = dropdowns[d];
            if (mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8
                    && mouseY >= topPos + dd.y && mouseY < topPos + dd.y + 14) {
                openDropdown = d;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean mouseClickedFormation(double mouseX, double mouseY, int button) {
        // 先处理已打开的下拉菜单
        if (openFormationDropdown >= 0) {
            List<ControlTerminalMenu.AircraftInfo> aircraft = menu.aircraftList();
            List<String> groupNames = getGroupNames();
            int ddi = openFormationDropdown;
            openFormationDropdown = -1;

            if (ddi < aircraft.size()) {
                int ay = topPos + MAP_Y + 14 + ddi * 24 + 16;
                int px = leftPos + 8;
                int rowH = 14;
                int itemIdx = 0;

                // "设为长机" 选项
                if (mouseY >= ay + itemIdx * rowH && mouseY < ay + (itemIdx + 1) * rowH) {
                    UUID uuid = aircraft.get(ddi).uuid();
                    PacketDistributor.sendToServer(new FormationCommandPayload(
                            FormationCommandPayload.SET_LEADER, uuid, ""));
                    return true;
                }
                itemIdx++;

                // 编组选项
                for (int gi = 0; gi < groupNames.size(); gi++) {
                    if (mouseY >= ay + itemIdx * rowH && mouseY < ay + (itemIdx + 1) * rowH) {
                        UUID uuid = aircraft.get(ddi).uuid();
                        PacketDistributor.sendToServer(new FormationCommandPayload(
                                FormationCommandPayload.ADD_TO_GROUP, uuid, groupNames.get(gi)));
                        return true;
                    }
                    itemIdx++;
                }

                // "移出编组" 选项
                if (mouseY >= ay + itemIdx * rowH && mouseY < ay + (itemIdx + 1) * rowH) {
                    UUID uuid = aircraft.get(ddi).uuid();
                    PacketDistributor.sendToServer(new FormationCommandPayload(
                            FormationCommandPayload.REMOVE_FROM_GROUP, uuid, ""));
                    return true;
                }
            }
            return true;
        }

        // 检查飞机行点击
        List<ControlTerminalMenu.AircraftInfo> aircraft = menu.aircraftList();
        for (int i = 0; i < aircraft.size(); i++) {
            int rowTop = topPos + MAP_Y + 14 + i * 24;
            if (mouseX >= leftPos + LEFT_X && mouseX < leftPos + LEFT_X + LEFT_W
                    && mouseY >= rowTop && mouseY < rowTop + 24) {
                openFormationDropdown = i;
                return true;
            }
        }

        // 检查新建编组按钮
        int btnY = topPos + MAP_Y + 14 + aircraft.size() * 24 + 4;
        if (mouseX >= leftPos + LEFT_X && mouseX < leftPos + LEFT_X + LEFT_W
                && mouseY >= btnY && mouseY < btnY + 14) {
            String nextName = "编组" + (getGroupNames().size() + 1);
            PacketDistributor.sendToServer(new FormationCommandPayload(
                    FormationCommandPayload.CREATE_GROUP, UUID.randomUUID(), nextName));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── 添加打击目标 ──

    private void onAddTarget() {
        try {
            double x = Double.parseDouble(inputX.getValue());
            double y = Double.parseDouble(inputY.getValue());
            double z = Double.parseDouble(inputZ.getValue());
            PacketDistributor.sendToServer(new AddTargetPayload(x, y, z));
            inputX.setValue("");
            inputY.setValue("");
            inputZ.setValue("");
        } catch (NumberFormatException ignored) {
            // 输入不完整，忽略
        }
    }

    // ── 渲染 ──

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + WIDTH, topPos + HEIGHT, BG_COLOR);
        g.fill(leftPos, topPos, leftPos + WIDTH, topPos + 12, HEADER_COLOR);
        g.fill(leftPos, topPos + 12, leftPos + WIDTH, topPos + 14, DIVIDER);

        if (showSettings) {
            renderSettingsBg(g);
        } else if (showFormation) {
            renderFormationBg(g);
        } else {
            renderMainBg(g);
        }
    }

    private void renderMainBg(GuiGraphics g) {
        int panelBottom = topPos + MAP_Y + MAP_H + 2;
        // 左栏面板 + 边框
        g.fill(leftPos + LEFT_X, topPos + MAP_Y, leftPos + LEFT_X + LEFT_W, panelBottom, PANEL_BG);
        drawBorder(g, leftPos + LEFT_X, topPos + MAP_Y, leftPos + LEFT_X + LEFT_W, panelBottom);
        // 中栏小地图 + 边框
        g.fill(leftPos + MAP_X, topPos + MAP_Y, leftPos + MAP_X + MAP_W, topPos + MAP_Y + MAP_H, MAP_BG);
        drawBorder(g, leftPos + MAP_X, topPos + MAP_Y, leftPos + MAP_X + MAP_W, topPos + MAP_Y + MAP_H);
        // 右栏面板 + 边框
        g.fill(leftPos + RIGHT_X, topPos + MAP_Y, leftPos + RIGHT_X + RIGHT_W, panelBottom, PANEL_BG);
        drawBorder(g, leftPos + RIGHT_X, topPos + MAP_Y, leftPos + RIGHT_X + RIGHT_W, panelBottom);
        // 底部分隔线
        g.fill(leftPos, topPos + BTN_AREA_Y - 2, leftPos + WIDTH, topPos + BTN_AREA_Y, DIVIDER);
    }

    private void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.fill(x1 - 1, y1 - 1, x2 + 1, y1, DIVIDER);   // 上
        g.fill(x1 - 1, y2, x2 + 1, y2 + 1, DIVIDER);     // 下
        g.fill(x1 - 1, y1, x1, y2, DIVIDER);              // 左
        g.fill(x2, y1, x2 + 1, y2, DIVIDER);              // 右
    }

    private void renderSettingsBg(GuiGraphics g) {
        g.fill(leftPos + 8, topPos + 16, leftPos + WIDTH - 8, topPos + 178, PANEL_BG);
    }

    private void renderFormationBg(GuiGraphics g) {
        g.fill(leftPos + 8, topPos + 16, leftPos + WIDTH - 8, topPos + 178, PANEL_BG);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (WIDTH - font.width(title)) / 2, 2, 0xFF50C4FF, false);

        if (showSettings) {
            renderSettingsLabels(g, mouseX, mouseY);
        } else if (showFormation) {
            renderFormationLabels(g, mouseX, mouseY);
        } else {
            renderMainLabels(g, mouseX, mouseY);
        }
    }

    // ── 主界面渲染 ──

    private void renderMainLabels(GuiGraphics g, int mouseX, int mouseY) {
        // 左栏标题（renderLabels 中坐标相对于 GUI 左上角）
        g.drawString(font, "§b飞机", LEFT_X + 2, MAP_Y + 2, 0xFF50C4FF, false);

        // 左栏飞机列表：只显示未编组的飞机和长机
        List<ControlTerminalMenu.AircraftInfo> aircraft = menu.aircraftList();
        UUID leaderId = menu.leaderUUID();
        int ay = MAP_Y + 14;
        int drawn = 0;
        int max = 6;
        for (int i = 0; i < aircraft.size() && drawn < max; i++) {
            ControlTerminalMenu.AircraftInfo info = aircraft.get(i);
            boolean isLeader = info.uuid().equals(leaderId);
            boolean hasGroup = menu.aircraftGroup(info.uuid()) != null;

            // 只显示未编组的飞机和长机
            if (!isLeader && hasGroup) continue;

            int rowY = ay + drawn * 16;
            // 状态色点
            g.fill(LEFT_X + 2, rowY + 1, LEFT_X + 4, rowY + 9, stateColor(info.state()));
            // 机型名 + 长机/编组标记
            String label = info.role().displayName();
            if (isLeader) label = "★" + label;
            g.drawString(font, label, LEFT_X + 7, rowY, TEXT_COLOR, false);
            // 弹药
            g.drawString(font, String.format("海%d空%d", info.seaAmmo(), info.airAmmo()),
                    LEFT_X + 7, rowY + 10, DIM_TEXT, false);
            drawn++;
        }
        if (drawn == 0) {
            g.drawString(font, "无飞机", LEFT_X + 2, ay, DIM_TEXT, false);
        }

        // 中栏：小地图
        renderMiniMap(g);

        // 右栏标题
        g.drawString(font, "§b打击坐标", RIGHT_X + 2, MAP_Y + 2, 0xFF50C4FF, false);

        // 坐标输入标签
        g.drawString(font, "X", RIGHT_X + 4, MAP_Y + 16, DIM_TEXT, false);
        g.drawString(font, "Y", RIGHT_X + 4, MAP_Y + 30, DIM_TEXT, false);
        g.drawString(font, "Z", RIGHT_X + 4, MAP_Y + 44, DIM_TEXT, false);

        // 盘旋点显示
        var rally = menu.rallyPoint();
        if (rally != null) {
            int ry = MAP_Y + MAP_H + 4;
            g.fill(RIGHT_X + 2, ry + 1, RIGHT_X + 4, ry + 9, MAP_RALLY);
            g.drawString(font, "盘旋", RIGHT_X + 7, ry, MAP_RALLY, false);
        }
    }

    // ── 小地图 ──

    private void renderMiniMap(GuiGraphics g) {
        // renderLabels 中坐标相对于 GUI 左上角，不需要 leftPos/topPos
        int mapLeft = MAP_X;
        int mapTop = MAP_Y;
        int mapRight = mapLeft + MAP_W;
        int mapBottom = mapTop + MAP_H;
        int centerX = mapLeft + MAP_W / 2;
        int centerZ = mapTop + MAP_H / 2;

        for (int i = 1; i < 4; i++) {
            int gx = mapLeft + i * MAP_W / 4;
            int gz = mapTop + i * MAP_H / 4;
            g.fill(gx, mapTop, gx + 1, mapBottom, MAP_GRID);
            g.fill(mapLeft, gz, mapRight, gz + 1, MAP_GRID);
        }
        g.fill(centerX, mapTop, centerX + 1, mapBottom, 0xFF333366);
        g.fill(mapLeft, centerZ, mapRight, centerZ + 1, 0xFF333366);

        Vec3 playerPos = menu.playerPosition();

        var rally = menu.rallyPoint();
        if (rally != null) {
            drawMapMarker(g, rally.x, rally.z, playerPos.x, playerPos.z, centerX, centerZ, mapLeft, mapTop, mapRight, mapBottom, MAP_RALLY, 3);
        }

        for (var t : menu.targetList()) {
            int color = t.isEntity() ? MAP_TARGET_ENT : MAP_TARGET;
            drawMapMarker(g, t.position().x, t.position().z, playerPos.x, playerPos.z, centerX, centerZ, mapLeft, mapTop, mapRight, mapBottom, color, 2);
        }

        for (var a : menu.aircraftList()) {
            drawMapMarker(g, a.position().x, a.position().z, playerPos.x, playerPos.z, centerX, centerZ, mapLeft, mapTop, mapRight, mapBottom, stateColor(a.state()), 2);
        }

        g.fill(centerX - 1, centerZ - 1, centerX + 2, centerZ + 2, MAP_PLAYER);

        g.fill(mapLeft - 1, mapTop - 1, mapRight + 1, mapTop, DIVIDER);
        g.fill(mapLeft - 1, mapBottom, mapRight + 1, mapBottom + 1, DIVIDER);
        g.fill(mapLeft - 1, mapTop, mapLeft, mapBottom, DIVIDER);
        g.fill(mapRight, mapTop, mapRight + 1, mapBottom, DIVIDER);

        g.drawString(font, String.format("%.0f格", MAP_SCALE * MAP_W / 2), mapLeft + 2, mapBottom + 2, DIM_TEXT, false);
    }

    private void drawMapMarker(GuiGraphics g, double worldX, double worldZ,
                               double playerX, double playerZ,
                               int centerX, int centerZ,
                               int mapLeft, int mapTop, int mapRight, int mapBottom,
                               int color, int size) {
        double dx = worldX - playerX;
        double dz = worldZ - playerZ;
        int screenX = centerX + (int) Math.round(dx / MAP_SCALE);
        int screenZ = centerZ + (int) Math.round(dz / MAP_SCALE);

        screenX = Math.max(mapLeft + 1, Math.min(mapRight - size - 1, screenX));
        screenZ = Math.max(mapTop + 1, Math.min(mapBottom - size - 1, screenZ));

        g.fill(screenX, screenZ, screenX + size, screenZ + size, color);
    }

    // ── 设置子页面渲染 ──

    private void renderSettingsLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, "§b设置", 8, 18, 0xFF50C4FF, false);

        drawSettingsDropdownRow(g, "自动锁定", autoLockName(), 28, 0, mouseX, mouseY);
        drawSettingsDropdownRow(g, "目标分配", assignName(), 44, 1, mouseX, mouseY);
        drawSettingsDropdownRow(g, "对空模式", airDefenseName(), 60, 2, mouseX, mouseY);
        drawSettingsDropdownRow(g, "投弹数量", String.valueOf(menu.bombsPerPass()), 76, 3, mouseX, mouseY);
        drawSettingsDropdownRow(g, "最小有效伤害", String.valueOf((int) menu.minEffDamage()), 92, 4, mouseX, mouseY);
    }

    private void drawSettingsDropdownRow(GuiGraphics g, String label, String value, int y, int ddIdx, int mouseX, int mouseY) {
        g.drawString(font, label, 16, y + 2, TEXT_COLOR, false);
        g.drawString(font, value, 100, y + 2, 0xFF90FF90, false);
        g.drawString(font, "▼", WIDTH - 24, y + 2, DIM_TEXT, false);
    }

    // ── 编组子页面渲染 ──

    private void renderFormationLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, "§b编组管理", 8, 18, 0xFF50C4FF, false);

        List<ControlTerminalMenu.AircraftInfo> aircraft = menu.aircraftList();
        UUID leaderId = menu.leaderUUID();

        for (int i = 0; i < aircraft.size(); i++) {
            ControlTerminalMenu.AircraftInfo info = aircraft.get(i);
            int rowY = MAP_Y + 14 + i * 24;

            // 机型名
            g.drawString(font, info.role().displayName(), LEFT_X + 7, rowY, TEXT_COLOR, false);
            // 长机 / 编组标记
            boolean isLeader = info.uuid().equals(leaderId);
            String group = menu.aircraftGroup(info.uuid());
            String statusText = isLeader ? "★长机" : (group != null ? "[" + group + "]" : "未编组");
            int statusColor = isLeader ? 0xFFFFD700 : (group != null ? 0xFF90FF90 : DIM_TEXT);
            g.drawString(font, statusText, LEFT_X + 7, rowY + 10, statusColor, false);
            // 弹药
            g.drawString(font, String.format("海%d空%d", info.seaAmmo(), info.airAmmo()),
                    LEFT_X + 50, rowY + 10, DIM_TEXT, false);
        }

        // 新建编组按钮
        int btnY = MAP_Y + 14 + aircraft.size() * 24 + 4;
        g.fill(LEFT_X, btnY, LEFT_X + LEFT_W, btnY + 12, DIVIDER);
        g.drawString(font, "+新建编组", LEFT_X + 10, btnY + 2, 0xFF50C4FF, false);

        // 渲染编组下拉菜单
        if (openFormationDropdown >= 0 && openFormationDropdown < aircraft.size()) {
            renderFormationDropdown(g, openFormationDropdown, mouseX, mouseY);
        }
    }

    private void renderFormationDropdown(GuiGraphics g, int aircraftIdx, int mouseX, int mouseY) {
        List<String> groupNames = getGroupNames();
        int itemCount = 1 + groupNames.size() + 1; // 设为长机 + groups + 移出编组
        int rowH = 14;
        int totalH = itemCount * rowH;
        int px = 8;
        int py = MAP_Y + 14 + aircraftIdx * 24 + 16;

        g.fill(px - 1, py - 1, px + LEFT_W + 1, py + totalH + 1, DROPDOWN_BORDER);
        g.fill(px, py, px + LEFT_W, py + totalH, DROPDOWN_BG);

        int itemIdx = 0;

        // "设为长机" / "取消长机"
        ControlTerminalMenu.AircraftInfo info = menu.aircraftList().get(aircraftIdx);
        boolean isLeader = info.uuid().equals(menu.leaderUUID());
        String leaderLabel = isLeader ? "取消长机" : "设为长机";
        drawDropdownItem(g, leaderLabel, px, py + itemIdx * rowH, LEFT_W, mouseX, mouseY);
        itemIdx++;

        // 编组选项
        for (String name : groupNames) {
            drawDropdownItem(g, "加入 [" + name + "]", px, py + itemIdx * rowH, LEFT_W, mouseX, mouseY);
            itemIdx++;
        }

        // "移出编组"
        drawDropdownItem(g, "移出编组", px, py + itemIdx * rowH, LEFT_W, mouseX, mouseY);
    }

    private void drawDropdownItem(GuiGraphics g, String text, int x, int y, int w, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 14;
        if (hover) g.fill(x, y, x + w, y + 14, DROPDOWN_HOVER);
        g.drawString(font, text, x + 4, y + 2, hover ? 0xFFFFEB3B : TEXT_COLOR, false);
    }

    private List<String> getGroupNames() {
        List<String> names = new ArrayList<>();
        for (var info : menu.aircraftList()) {
            String group = menu.aircraftGroup(info.uuid());
            if (group != null && !names.contains(group)) {
                names.add(group);
            }
        }
        return names;
    }

    // ── 主渲染入口 ──

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateButtonVisibility();
        super.render(g, mouseX, mouseY, partialTick);

        if (showSettings && openDropdown >= 0) {
            renderDropdown(g, dropdowns[openDropdown], mouseX, mouseY);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics g, DropdownDef dd, int mouseX, int mouseY) {
        int px = leftPos + 80;
        int py = topPos + dd.y + 14;
        int rowH = 14;
        int totalH = dd.options.length * rowH;
        int current = getCurrentOrdinal(dd.currentGetter);

        g.fill(px - 1, py - 1, px + 161, py + totalH + 1, DROPDOWN_BORDER);
        g.fill(px, py, px + 160, py + totalH, DROPDOWN_BG);

        for (int i = 0; i < dd.options.length; i++) {
            int ry = py + i * rowH;
            boolean hover = mouseX >= px && mouseX < px + 160 && mouseY >= ry && mouseY < ry + rowH;
            boolean selected = i == current;
            if (selected) g.fill(px, ry, px + 160, ry + rowH, DROPDOWN_SEL);
            else if (hover) g.fill(px, ry, px + 160, ry + rowH, DROPDOWN_HOVER);
            int textColor = selected ? 0xFF90FF90 : (hover ? 0xFFFFEB3B : TEXT_COLOR);
            g.drawString(font, dd.options[i], px + 4, ry + 2, textColor, false);
            if (selected) g.drawString(font, "✓", px + 148, ry + 2, 0xFF90FF90, false);
        }
    }

    // ── 键盘输入 ──

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 优先让坐标输入框处理键盘事件
        if (inputX.isFocused() && inputX.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (inputY.isFocused() && inputY.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (inputZ.isFocused() && inputZ.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── 网络 ──

    private void sendCommand(byte action) {
        PacketDistributor.sendToServer(new CommandPayload(action));
    }

    // ── 名称映射 ──

    private String autoLockName()   { int i = menu.autoLockOrdinal();   return i >= 0 && i < AUTO_LOCK_NAMES.length   ? AUTO_LOCK_NAMES[i]   : "?"; }
    private String assignName()     { int i = menu.assignmentOrdinal(); return i >= 0 && i < ASSIGN_NAMES.length      ? ASSIGN_NAMES[i]      : "?"; }
    private String airDefenseName() { int i = menu.airDefenseOrdinal(); return i >= 0 && i < AIR_DEFENSE_NAMES.length ? AIR_DEFENSE_NAMES[i] : "?"; }

    private int getCurrentOrdinal(int getter) {
        return switch (getter) {
            case 0 -> menu.autoLockOrdinal();
            case 1 -> menu.assignmentOrdinal();
            case 2 -> menu.airDefenseOrdinal();
            case 3 -> Math.max(0, menu.bombsPerPass() - 1);
            case 4 -> minDmgOrdinal(menu.minEffDamage());
            default -> 0;
        };
    }

    private static int minDmgOrdinal(float v) {
        if (v <= 0)  return 0;
        if (v <= 20) return 1;
        if (v <= 40) return 2;
        return 3;
    }

    // ── 状态颜色 ──

    private static int stateColor(AircraftState state) {
        return switch (state) {
            case STANDBY, ORBITING          -> 0xFF4CAF50;
            case LOCKED, APPROACH, ATTACKING -> 0xFFFF9800;
            case DROPPING                    -> 0xFFF44336;
            case POST_ATTACK                 -> 0xFFFFEB3B;
            case DOGFIGHT                    -> 0xFFE91E63;
            case RETURNING                   -> 0xFF2196F3;
        };
    }
}
