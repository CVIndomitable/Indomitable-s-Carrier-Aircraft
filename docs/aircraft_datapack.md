# 机型配置数据包

## 概述

机型配置使用 JSON 数据包，路径为：
```
data/<namespace>/aircraft/<aircraft_id>.json
```

模组会先加载内置的 4 个默认机型（`b25`、`btd`、`rocket_attacker`、`asw_patrol`），然后加载数据包中的配置（可以覆盖内置机型或添加新机型）。

## JSON 格式

```json
{
  "speed": 0.55,
  "standby_height": 22.0,
  "attack_height": 36.0,
  "attack_range": 18.0,
  "turn_distance": 64.0,
  "health": 60.0,
  "sea_ammo_capacity": 6,
  "magazine_capacity": 1000,
  "burst_size": 2,
  "weapon_damage": 30.0,
  "explosion_radius": 4.0,
  "turret_range": 28.0,
  "asw_range": 0.0,
  "air_weapon_mode": "both",
  "sea_attack_modes": [
    "level_bombing"
  ],
  "allowed_sea_ammo": [
    "aerial_bomb"
  ]
}
```

## 字段说明

### 基础属性

- `speed` (number): 飞行速度（方块/tick）
- `standby_height` (number): 待命高度（相对玩家）
- `attack_height` (number): 攻击高度（相对目标）
- `attack_range` (number): 攻击距离
- `turn_distance` (number): 转向距离
- `health` (number): 生命值

### 武器属性

- `sea_ammo_capacity` (integer): 对海弹药容量
- `magazine_capacity` (integer): 对空弹匣容量（0 表示无对空能力）
- `burst_size` (integer): 每次射击的弹丸数
- `weapon_damage` (number): 武器伤害
- `explosion_radius` (number): 爆炸半径

### 特殊能力

- `turret_range` (number): 炮塔射程（0 表示无炮塔）
- `asw_range` (number): 反潜探测范围（0 表示无反潜能力）

### 武器配置

- `air_weapon_mode` (enum): 对空武器模式
  - `"none"` - 无对空能力
  - `"forward"` - 前射机枪
  - `"turret"` - 自卫炮塔
  - `"both"` - 前射 + 炮塔

- `sea_attack_modes` (array of enum): 对海攻击模式（至少一个）
  - `"level_bombing"` - 水平轰炸
  - `"dive_bombing"` - 俯冲轰炸
  - `"torpedo"` - 鱼雷攻击
  - `"rocket"` - 火箭攻击
  - `"asw_bombing"` - 反潜轰炸

- `allowed_sea_ammo` (array of enum): 允许的对海弹药类型（至少一个）
  - `"aerial_bomb"` - 航空炸弹
  - `"aerial_torpedo"` - 航空鱼雷
  - `"rocket"` - 火箭弹

## 内置机型

### B-25（水平轰炸机）
- 文件：`data/indomitablecarrieraircraft/aircraft/b25.json`
- 特点：前射机枪 + 自卫炮塔，航空炸弹，水平轰炸

### BTD（俯冲轰炸机/鱼雷机）
- 文件：`data/indomitablecarrieraircraft/aircraft/btd.json`
- 特点：前射机枪，支持航弹和鱼雷，可俯冲轰炸

### 火箭攻击机
- 文件：`data/indomitablecarrieraircraft/aircraft/rocket_attacker.json`
- 特点：前射机枪，火箭弹

### 反潜巡逻机
- 文件：`data/indomitablecarrieraircraft/aircraft/asw_patrol.json`
- 特点：无对空能力，航空炸弹，反潜探测

## 添加自定义机型

1. 在你的数据包中创建目录：`data/<your_namespace>/aircraft/`
2. 创建 JSON 文件：`<aircraft_id>.json`
3. 填写机型配置（所有字段都是必需的）
4. 使用 `/reload` 命令重新加载数据包

## 注意事项

- 所有字段都是必需的，缺少任何字段会导致加载失败
- 枚举值不区分大小写，但建议使用小写
- 数组字段（`sea_attack_modes`、`allowed_sea_ammo`）至少需要一个有效值
- 无效的枚举值会被忽略并记录警告
- 数据包可以覆盖内置机型（使用相同的 `aircraft_id`）
