# AcademyCraft-Return

一个关于学院都市的 Minecraft 模组,适用于 Forge。

本仓库的 `port/1.20.1` 分支以原版 AcademyCraft 为历史基线，并整合
[AcademyCraft-Return](https://github.com/lom2333/AcademyCraft-Return) 的 Forge 1.20.1
移植成果。迁移范围、构建要求和验证结果见 [MIGRATION_1_20_1.md](MIGRATION_1_20_1.md)。

项目当前处于 **Beta 测试**,可正常游玩。欢迎各位玩家前来反馈。

- 游戏版本:Minecraft **1.20.1**
- 加载器:Forge **47.x**
- 作者(移植):**lom233**
- 原作者:**LambdaInnovation**([原仓库](https://github.com/LambdaInnovation/AcademyCraft))

## 构建

需要 **JDK 17**。克隆仓库后在项目根目录执行:

```bash
./gradlew build
```

Windows 下用:

```bash
gradlew.bat build
```

首次构建会自动下载 Forge/Minecraft 依赖,耗时较长;完成后 mod 文件在 `build/libs/` 目录。

开发调试可直接启动游戏客户端:

```bash
./gradlew runClient
```

## 许可证 / License

本项目是对 AcademyCraft 的 1.20.1 移植与二次开发,遵循原项目的许可结构:

- **游戏内容部分**(`cn.academy.*` 包):**GPL-3.0-or-later**,并遵循原作者附加条款 —— 禁止以任何形式出售 AcademyCraft 及其内容。
- **底层库部分**(源自 [LambdaLib2](https://github.com/LambdaInnovation/LambdaLib2),`cn.lambdalib2.*` 包):**MIT** 协议,原作者 **WeAthFolD / LambdaInnovation**,此处保留其署名。

对外分发时整体以 **GPLv3** 发布(见 [LICENSE](LICENSE)),**完全免费,禁止任何形式的收费/出售**。素材(贴图/模型/音效)源自原仓库,版权归原作者所有。
