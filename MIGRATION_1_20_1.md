# AcademyCraft 1.20.1 迁移说明

## 目标环境

- Minecraft 1.20.1
- Forge 47.3.0（兼容 Forge 47.x）
- Java 17
- Gradle 8.1.1 / ForgeGradle 6
- Official mappings 1.20.1

## 迁移基线

本分支从 `LambdaInnovation/AcademyCraft` 的 1.12.2 源码开始，整合了
`lom2333/AcademyCraft-Return` 的 `Forge-1.20.1` 分支（基线提交
`3e2c5c53`，版本 `1.2.5fix-Beta`）。原项目内容遵循 GPLv3 及原作者附加条款；
移植内包含的 LambdaLib2 兼容层保留 MIT 许可和原作者署名。

原始 1.12.2 源码仍保留在 `master` 分支，1.20.1 迁移位于
`port/1.20.1` 分支。

## 主要迁移内容

- 构建从 ForgeGradle 2.3、Java 8 和 Scala 混编迁到 ForgeGradle 6、Java 17。
- 方块、物品、实体、流体、粒子、音效、菜单和方块实体改为 Forge
  `DeferredRegister` 注册体系。
- 玩家能力数据迁到 Forge Capability，世界级数据迁到 `SavedData`。
- 网络层迁到 Forge `SimpleChannel`，保留 LambdaLib2 的序列化兼容接口。
- GUI、HUD、方块实体渲染、实体特效、粒子、Shader 和 39 个 Mixin 适配
  1.20.1 客户端/服务端类结构。
- 保留五种能力类别：电击使、原子崩坏、心理掌握、空间移动和矢量操控。
- 保留能源网络、开发机、金属成型机、相位发生器、虚相位液体、无线节点、
  教程、成就以及可选 JEI 集成。
- 资源包升级到 1.20.1 的 JSON 语言、模型、方块状态、Shader 和数据包格式。

## 构建

Windows：

```powershell
.\gradlew.bat build --no-daemon
```

构建产物：

```text
build/libs/academycraft-1.20.1-1.01.jar
```

## 当前验证结果

- `gradlew.bat build --no-daemon`：通过。
- Java 源码编译、资源处理、Mixin refmap、Forge reobf、打包：通过。
- 350 个 JSON 资源文件语法校验：通过。
- 专用服务端开发启动：Forge 47.3.0 成功识别
  `academy 1.2.5fix-Beta`，39 个 Mixin 完成准备，并正常运行到 EULA 检查。
- `1.2.5fix-Beta.1` 恢复原版 CP/OL 规则：技能扣除 CP 后会触发配置的
  回复延迟，LV5 生存玩家也会正常累积 OL。
- 未自动接受 Mojang EULA；完整世界创建和真实客户端交互仍需由使用者手动验证。

## 已知边界

- 当前版本仍是 Beta。构建和加载冒烟测试不能替代所有技能、机器、多人同步和
  显卡/Shader 组合的游戏内回归测试。
- JEI 是可选编译依赖；不安装 JEI 不影响 AcademyCraft 本体加载。
- 运行开发客户端或服务端必须使用 Java 17，不能使用原项目所需的 Java 8。
