# 机型配置数据包示例

本示例展示如何通过数据包添加自定义机型或修改现有机型。

## 示例 1：添加新的自定义机型

创建一个高速侦察机，特点是速度快、血量低、无对海能力但有强大的对空能力。

**文件路径**：`data/mypack/aircraft/scout.json`

```json
{
  "speed": 0.85,
  "standby_height": 25.0,
  "attack_height": 30.0,
  "attack_range": 25.0,
  "turn_distance": 72.0,
  "health": 30.0,
  "sea_ammo_capacity": 0,
  "magazine_capacity": 2000,
  "burst_size": 3,
  "weapon_damage": 20.0,
  "explosion_radius": 0.0,
  "turret_range": 0.0,
  "asw_range": 0.0,
  "air_weapon_mode": "forward",
  "sea_attack_modes": [
    "level_bombing"
  ],
  "allowed_sea_ammo": [
    "aerial_bomb"
  ]
}
```

**说明**：
- `sea_ammo_capacity: 0` — 无对海弹药
- `magazine_capacity: 2000` — 双倍对空弹药
- `speed: 0.85` — 比其他机型都快
- `burst_size: 3` — 每次射击 3 发子弹

## 示例 2：修改现有机型（增强 B-25）

覆盖内置的 B-25 配置，增加其血量和弹药容量。

**文件路径**：`data/mypack/aircraft/b25.json`

```json
{
  "speed": 0.55,
  "standby_height": 22.0,
  "attack_height": 36.0,
  "attack_range": 18.0,
  "turn_distance": 64.0,
  "health": 100.0,
  "sea_ammo_capacity": 12,
  "magazine_capacity": 2000,
  "burst_size": 3,
  "weapon_damage": 35.0,
  "explosion_radius": 5.0,
  "turret_range": 32.0,
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

**变更**：
- `health`: 60.0 → 100.0
- `sea_ammo_capacity`: 6 → 12
- `magazine_capacity`: 1000 → 2000
- `burst_size`: 2 → 3
- `weapon_damage`: 30.0 → 35.0
- `explosion_radius`: 4.0 → 5.0
- `turret_range`: 28.0 → 32.0

## 示例 3：重型轰炸机

创建一个重型轰炸机，速度慢但载弹量大，爆炸威力强。

**文件路径**：`data/mypack/aircraft/heavy_bomber.json`

```json
{
  "speed": 0.40,
  "standby_height": 30.0,
  "attack_height": 50.0,
  "attack_range": 20.0,
  "turn_distance": 80.0,
  "health": 100.0,
  "sea_ammo_capacity": 16,
  "magazine_capacity": 500,
  "burst_size": 1,
  "weapon_damage": 50.0,
  "explosion_radius": 8.0,
  "turret_range": 35.0,
  "asw_range": 0.0,
  "air_weapon_mode": "turret",
  "sea_attack_modes": [
    "level_bombing"
  ],
  "allowed_sea_ammo": [
    "aerial_bomb"
  ]
}
```

**特点**：
- 速度慢（0.40）但转向距离长（80.0）
- 高血量（100.0）
- 大载弹量（16）
- 高伤害（50.0）和大爆炸范围（8.0）
- 仅有自卫炮塔，无前射武器（`"turret"`）

## 如何使用自定义机型

1. 将 JSON 文件放入数据包的 `data/<namespace>/aircraft/` 目录
2. 在游戏中使用 `/reload` 命令重新加载数据包
3. 查看日志确认加载成功：`Loaded aircraft spec: <aircraft_id>`

注意：目前自定义机型需要通过代码或命令来召唤，物品注册系统尚未支持动态机型。

## 参数调优建议

### 速度与转向
- `speed` 越高，`turn_distance` 应越大（高速机型需要更大的转弯半径）
- 建议比例：`turn_distance ≈ speed × 100 ± 10`

### 攻击高度
- 水平轰炸机：`attack_height` 应较高（30-50）以便有足够的投弹距离
- 俯冲轰炸机：`attack_height` 应较低（15-25）
- 火箭机：`attack_height` 应更低（10-20）

### 平衡性
- 高速机型通常应有较低的血量和载弹量
- 重型机型可以有高血量和高载弹量，但速度应较慢
- 对空能力越强，对海能力通常应越弱（或反之）

## 枚举值参考

### air_weapon_mode
- `none` - 无对空能力（反潜机、运输机）
- `forward` - 前射机枪（战斗机、俯冲轰炸机）
- `turret` - 自卫炮塔（重型轰炸机）
- `both` - 前射 + 炮塔（多用途轰炸机）

### sea_attack_modes
- `level_bombing` - 水平轰炸
- `dive_bombing` - 俯冲轰炸
- `torpedo` - 鱼雷攻击
- `rocket` - 火箭攻击
- `asw_bombing` - 反潜轰炸

### allowed_sea_ammo
- `aerial_bomb` - 航空炸弹
- `aerial_torpedo` - 航空鱼雷
- `rocket` - 火箭弹
