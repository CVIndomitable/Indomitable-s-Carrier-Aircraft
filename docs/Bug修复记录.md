# Bug修复记录

## Bug #1: 飞机无法召唤

**日期**: 2026-06-02  
**发现阶段**: Phase 1 游戏内测试  
**严重程度**: P0 - 阻断功能

### 症状
- 右键使用召唤器显示"已召唤水平轰炸机"
- 但游戏中没有任何实体出现
- F3显示实体数量没有增加

### 根本原因
`EntityAttributeCreationEvent` 使用了错误的事件总线：

```java
// ❌ 错误
@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class CommonModEvents {
    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.BOMBER_AIRCRAFT.get(), ...);
    }
}
```

**分析**:
- `EntityAttributeCreationEvent` 是 **MOD 总线事件**
- 我们错误地将其注册到 GAME 总线
- 导致事件处理器从未被调用
- 飞机实体的属性（MAX_HEALTH, FLYING_SPEED等）未注册
- Minecraft尝试创建实体时因缺少属性而失败

### 修复方案

```java
// ✅ 正确
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CommonModEvents {
    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.BOMBER_AIRCRAFT.get(), BomberAircraftEntity.createAttributes().build());
        LOGGER.info("Registered bomber aircraft entity attributes");
    }
}
```

同时修复了 `ClientModEvents`:
```java
@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
```

### 附加改进
1. 添加日志输出以验证注册成功
2. 改进召唤反馈消息，显示实体ID和成功状态
3. 在召唤器中添加调试日志

### 验证方法
1. 重新编译并启动游戏
2. 查看启动日志确认 "Registered bomber aircraft entity attributes"
3. 使用召唤器，检查消息显示 "添加成功: true"
4. 按F3+B显示碰撞箱，确认飞机实体存在

### 教训
- **NeoForge事件总线分类**:
  - **MOD总线**: 注册相关事件（实体、方块、物品、属性等）
  - **GAME总线**: 游戏逻辑事件（玩家交互、世界事件等）
- 使用错误的总线会导致事件处理器静默失败
- 关键注册代码应添加日志验证

### 影响范围
- **修复文件**: 2个
  - `CommonModEvents.java`
  - `ClientModEvents.java`
- **测试状态**: 待游戏内验证

---

**状态**: ✅ 已修复，待验证  
**提交**: [commit hash]
