# 不挠的舰载机 - 项目结构

## 目录结构

```
不挠的舰载机/
├── src/
│   ├── main/
│   │   ├── java/com/indomitable/carrieraircraft/
│   │   │   ├── IndomitableCarrierAircraft.java    # 主类
│   │   │   ├── registry/                          # 注册器包
│   │   │   │   ├── ModBlocks.java                # 方块注册
│   │   │   │   ├── ModItems.java                 # 物品注册
│   │   │   │   ├── ModCreativeTabs.java          # 创造标签页
│   │   │   │   ├── ModEntityTypes.java           # 实体类型注册
│   │   │   │   ├── ModDataComponents.java        # 数据组件注册
│   │   │   │   ├── ModMenuTypes.java             # GUI菜单类型
│   │   │   │   └── ModSounds.java                # 音效注册
│   │   │   ├── entity/                            # 实体包（待实现）
│   │   │   │   ├── aircraft/                     # 飞机实体
│   │   │   │   └── projectile/                   # 弹药实体
│   │   │   ├── combat/                            # 战斗系统
│   │   │   │   ├── BallisticSolver.java          # 弹道解算引擎（已移植）
│   │   │   │   └── HitNotifier.java              # 命中通知系统（已移植）
│   │   │   ├── event/                             # 事件处理
│   │   │   │   ├── CommonModEvents.java          # Mod事件
│   │   │   │   └── ServerGameEvents.java         # 游戏事件
│   │   │   ├── client/                            # 客户端代码
│   │   │   │   └── ClientModEvents.java          # 客户端事件
│   │   │   ├── network/                           # 网络包（待实现）
│   │   │   └── config/                            # 配置（待实现）
│   │   ├── resources/
│   │   │   ├── assets/indomitablecarrieraircraft/
│   │   │   │   ├── lang/                         # 语言文件
│   │   │   │   │   ├── zh_cn.json               # 中文
│   │   │   │   │   └── en_us.json               # 英文
│   │   │   │   ├── textures/                     # 材质（待添加）
│   │   │   │   ├── models/                       # 模型（待添加）
│   │   │   │   └── sounds/                       # 音效（待添加）
│   │   │   └── data/indomitablecarrieraircraft/
│   │   │       ├── aircraft_types/               # 机型配置（数据驱动）
│   │   │       └── tags/                         # 标签定义
│   │   └── templates/
│   │       └── META-INF/
│   │           └── neoforge.mods.toml            # Mod元数据模板
│   └── generated/                                 # 自动生成的资源
├── build.gradle                                   # Gradle构建脚本
├── gradle.properties                              # Gradle属性配置
├── settings.gradle                                # Gradle设置
├── CLAUDE.md                                      # 项目文档和计划
└── README.md                                      # 项目说明

## 核心模块说明

### 1. 注册器（registry/）
所有游戏内容的注册中心：
- **ModEntityTypes**: 飞机实体、弹药实体
- **ModItems**: 飞机召唤物品、弹药物品
- **ModBlocks**: 控制面板方块
- **ModDataComponents**: 飞机状态、弹药数据
- **ModSounds**: 引擎声、爆炸声等

### 2. 战斗系统（combat/）
从皮兰港移植的核心代码：
- **BallisticSolver**: 高精度弹道解算引擎
  - 网格搜索 + 三分法
  - LRU缓存优化
  - 支持阻力、重力、高低差
- **HitNotifier**: 玩家命中通知系统

### 3. 事件处理（event/）
- **CommonModEvents**: 实体属性注册等
- **ServerGameEvents**: 服务器tick、飞机AI更新

### 4. 客户端（client/）
- **ClientModEvents**: 实体渲染器注册
- 后续添加：GUI渲染、火控面板等

## 开发阶段

### Phase 1 (当前) - MVP
- [ ] 水平轰炸机实体
- [ ] 自由落体炸弹
- [ ] 基础状态机
- [ ] 火控系统
- [ ] 召唤物品

### Phase 2 - 多机型与弹道
- [ ] 双武器槽系统
- [ ] 数据驱动机型配置
- [ ] 俯冲轰炸
- [ ] 鱼雷攻击
- [ ] 基础空战

### Phase 3 - 编组与控制面板
- [ ] 三级编组系统
- [ ] 控制面板GUI
- [ ] 多机协同

### Phase 4 - 高级功能
- [ ] 长机视角
- [ ] 区块强制加载
- [ ] 反潜探测
- [ ] 完整锁定系统

### Phase 5 - 优化与打磨
- [ ] 性能优化
- [ ] 音效与粒子
- [ ] 配置界面

## 从皮兰港移植的技术

1. **弹道解算系统** - 用于精确计算投弹时机
2. **实体同步架构** - 确保客户端与服务端一致
3. **火控系统基础** - 目标锁定与追踪
4. **配置系统模式** - COMMON配置热重载

## 技术栈

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.220
- **Java**: 21
- **Gradle**: 9.2.1
- **Parchment Mappings**: 2024.11.17
