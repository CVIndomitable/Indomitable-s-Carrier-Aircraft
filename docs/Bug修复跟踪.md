# Phase 1 Bug修复跟踪

## Bug #1: 实体属性未注册 ✅ 已修复
**提交**: b14b7e3

**问题**: EntityAttributeCreationEvent 使用了错误的事件总线(GAME)
**修复**: 改为 Bus.MOD
**验证**: 日志显示 "Registered bomber aircraft entity attributes"

---

## Bug #2: 飞机被立即移除 ✅ 已修复
**提交**: fe31ae5

**问题**: 
- tickStandby() 使用 `level().getEntity(UUID)` 获取玩家
- 该方法只查找实体，无法找到玩家
- 导致 owner == null，触发 discard()

**修复**: 
```java
// 错误
var owner = ((ServerLevel) this.level()).getEntity(ownerUUID);

// 正确
var owner = ((ServerLevel) this.level())
              .getServer()
              .getPlayerList()
              .getPlayer(ownerUUID);
```

**调试增强**:
- 添加警告日志：owner not found 时输出
- 添加tick日志：每秒输出位置、状态、弹药

**验证方法**:
1. 召唤飞机后立即抬头看天空
2. 按F3+B显示碰撞箱
3. 检查日志是否有 "Bomber aircraft XXX at (x,y,z) state: STANDBY"

---

## 待测试
- [ ] 飞机是否能正常存在（不被立即移除）
- [ ] 飞机是否在玩家头顶盘旋
- [ ] 锁定目标后飞机是否能飞向目标
- [ ] 投弹功能
- [ ] 返航逻辑

---

## 可能的后续问题
1. **物品模型缺失** - 警告但不影响功能
2. **飞行路径可能不平滑** - 需要观察
3. **投弹精度** - 需要测试验证
