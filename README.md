# 不挠的舰载机 (Indomitable's Carrier Aircraft)

一个功能完整的 Minecraft 舰载机系统模组。

## 项目信息

- **MC 版本**: 1.21.1
- **加载器**: NeoForge 21.1.220
- **Mod ID**: `indomitablecarrieraircraft`
- **状态**: 可编译核心玩法版

## 当前功能

- [x] **数据驱动机型系统**：所有机型参数由 JSON 数据包配置，支持热重载（`/reload`）
- [x] 多机型召唤：B-25、BTD、BTD鱼雷型、火箭攻击机、反潜巡逻机
- [x] 完整状态机骨架：STANDBY、ORBITING、LOCKED、APPROACH、ATTACKING、DROPPING、POST_ATTACK、DOGFIGHT、RETURNING
- [x] 三坐标打击流程：目标点、弹着点、投放点、转向点
- [x] 弹药类型：机枪弹匣、航空炸弹、航空鱼雷、航空火箭弹
- [x] 火控系统：最多4个实体/坐标目标，支持集火/均衡分配
- [x] 自动锁定：最近、威胁最高、集火、分散、类型过滤
- [x] 编组基础：玩家飞机索引、长机查询、盘旋点部署、全机召回
- [x] 反潜探测：反潜机周期性高亮水下目标
- [x] 控制终端：无 GUI 可玩控制面板，用于锁定、盘旋点、召回和配置切换

## 数据包支持

模组支持通过数据包添加或修改机型配置。详细说明：

- **配置格式**：`docs/aircraft_datapack.md`
- **示例配置**：`docs/aircraft_datapack_examples.md`
- **数据包路径**：`data/<namespace>/aircraft/<aircraft_id>.json`

内置 4 个默认机型（b25、btd、rocket_attacker、asw_patrol），数据包可以覆盖或添加新机型。

## 开发计划

详见 `CLAUDE.md` 中的分阶段开发规划。

## 从皮兰港项目移植的代码

- `BallisticSolver` - 弹道解算引擎（用于计算投弹时机）
- `HitNotifier` - 命中通知系统

## 构建

```bash
./gradlew build
```

沙箱内已验证 `./gradlew compileJava` 通过。完整 `build` 可能需要访问用户目录中的 Gradle wrapper/cache 锁文件。

## 运行

```bash
./gradlew runClient  # 客户端
./gradlew runServer  # 服务器
```

## 开发

使用 IntelliJ IDEA 或其他支持 Gradle 的 IDE 打开项目。

## 许可

All Rights Reserved
