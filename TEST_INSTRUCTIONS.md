# 飞机召唤测试说明

## 修改内容
已添加详细的调试日志，用于诊断飞机无法召唤的问题。

### 修改的文件
1. `IndomitableCarrierAircraft.java` - 主类初始化日志
2. `ModEntityTypes.java` - 实体类型注册日志
3. `CommonModEvents.java` - 实体属性注册详细日志
4. `AircraftSpawnerItem.java` - 召唤过程详细日志

## 测试步骤

### 1. 启动游戏
启动游戏并观察日志输出，应该看到以下关键信息：

```
=== Indomitable's Carrier Aircraft Initialization START ===
Registering DataComponents...
Registering Blocks...
Registering Items...
Registering Creative Tabs...
Registering Menu Types...
Registering Entity Types...
Creating BOMBER_AIRCRAFT entity type
Registering Sounds...
=== Indomitable's Carrier Aircraft Initialization COMPLETE ===

=== EntityAttributeCreationEvent FIRED ===
Got bomber aircraft entity type: ...
Created attributes: ...
✅ Successfully registered bomber aircraft entity attributes
```

**如果看不到 "EntityAttributeCreationEvent FIRED"，说明事件总线有问题！**

### 2. 进入创造模式世界
- 创建一个新的超平坦世界
- 切换到创造模式

### 3. 获取召唤器
- 按 `E` 打开背包
- 搜索 "bomber" 或查看 Indomitable 标签页
- 获取 "Bomber Spawner"（水平轰炸机召唤器）

### 4. 测试召唤
- 主手拿着召唤器
- **右键点击**（不是左键）
- 观察：
  - 聊天框应该显示：`已召唤水平轰炸机 (实体ID: xxx, 添加成功: true)`
  - 抬头看，应该能在头顶20格处看到飞机实体
  - 按 `F3 + B` 显示碰撞箱，确认实体存在

### 5. 查看日志
无论成功与否，日志中应该有：

```
=== AIRCRAFT SPAWN REQUESTED ===
Spawn position: Vec3(x, y, z)
Aircraft entity created successfully: ...
Target set: ... (如果之前设置过目标)
addFreshEntity result: true/false
✅ Aircraft spawned successfully at ... (成功)
或
❌ Failed to add aircraft entity to world (失败)
```

## 可能的问题

### 问题1: 看不到 "EntityAttributeCreationEvent FIRED"
**原因**: 事件总线配置错误
**解决**: 检查 `CommonModEvents.java` 的 `@EventBusSubscriber` 注解是否正确

### 问题2: "addFreshEntity result: false"
**原因**: 实体创建失败，可能是属性未注册或实体构造有问题
**检查**: 
- 确认属性注册成功
- 查看是否有异常堆栈

### 问题3: 游戏崩溃或异常
**查看**: 日志中的 "❌ Exception during aircraft spawn" 及其堆栈信息

## 下一步

测试完成后，请告知：
1. 是否看到了所有关键日志
2. `addFreshEntity result` 的值
3. 是否能看到飞机实体
4. 如有错误，请提供完整的日志输出
