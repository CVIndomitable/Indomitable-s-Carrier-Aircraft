# 不挠的舰载机 (Indomitable's Carrier Aircraft)

一个功能完整的 Minecraft 舰载机系统模组。

## 项目信息

- **MC 版本**: 1.21.1
- **加载器**: NeoForge 21.1.220
- **Mod ID**: `indomitablecarrieraircraft`
- **状态**: Phase 1 (MVP 开发中)

## 当前功能（Phase 1 目标）

- [ ] 单一机型：水平轰炸机
- [ ] 基础状态机：STANDBY、APPROACH、DROPPING、RETURNING
- [ ] 简单弹道：自由落体炸弹（使用弹道解算引擎）
- [ ] 火控系统：玩家锁定单一目标
- [ ] 基础物品：飞机召唤物品、炸弹

## 开发计划

详见 `CLAUDE.md` 中的分阶段开发规划。

## 从皮兰港项目移植的代码

- `BallisticSolver` - 弹道解算引擎（用于计算投弹时机）
- `HitNotifier` - 命中通知系统

## 构建

```bash
./gradlew build
```

## 运行

```bash
./gradlew runClient  # 客户端
./gradlew runServer  # 服务器
```

## 开发

使用 IntelliJ IDEA 或其他支持 Gradle 的 IDE 打开项目。

## 许可

All Rights Reserved
