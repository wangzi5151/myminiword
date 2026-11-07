# 我的迷你世界 VoxelCraft

一款使用 Java + OpenGL ES 2.0 编写的 Android 体素沙盒游戏。世界由无限程序化地形、城市、车站、铁轨、村民和可编辑方块组成，支持生存式挖掘/放置、飞行、矿车、背包合成以及实时阴影、泛光等画面效果。

- 包名：`com.wangzi5151.myminiword`
- 最低系统：Android 8.0（API 26）
- 目标 SDK：34
- 渲染：OpenGL ES 2.0

## 构建

需要 JDK 17、Android SDK（`compileSdk 34`）和可用的 `aapt2`。

```bash
export ANDROID_HOME=/path/to/android-sdk
bash gradlew assembleDebug
```

Termux / 非标准 SDK 环境可使用 aapt2 覆盖参数：

```bash
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
bash gradlew assembleDebug \
  -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

运行 JVM 单元测试：

```bash
bash gradlew testDebugUnitTest \
  -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Debug APK 输出在 `app/build/outputs/apk/debug/`。

## 操作

- 左侧摇杆：前后左右移动。
- 右侧屏幕拖动：转动视角。
- “跳跃”：跳跃 / 飞行时上升。
- “挖掘”：长按破坏准星指向的方块。
- “放置”：把当前快捷栏方块放到准星指向的位置。
- “飞行 / 行走”：切换飞行模式。
- “矿车 / 下车”：在铁轨附近骑乘或离开矿车。
- 底部快捷栏：点击切换方块，共 16 格。
- “背包”：查看物品、选择方块并按配方合成。
- “设置”：调整视距、灵敏度、云层、性能面板、时间流动、实时阴影、画质增强、时间预设，或重置世界。

## 实现概览

- `world/ChunkMesh`：不透明方块使用贪心网格合并，玻璃/水等使用逐面网格；单个网格顶点超过 65535 时自动拆分为多个 `MeshData.Part`。
- `world/MeshData`：每个 part 使用独立的顶点数组和 `short` 索引，保证单个 part 的索引不会回绕。
- `world/World`：区块生成 / 网格化工作线程、区块上传、编辑记录、存档、DDA 射线检测。
- `GameRenderer`：按 part 执行多个 draw call，阴影和主渲染路径均支持拆分后的网格。
- 存档：编辑快照在单线程执行器中异步写盘，`saveAndWait` / `flushSave` 可等待完成；暂停时会等待存档落盘。

## 测试

`app/src/test` 下的 JVM 单元测试覆盖：

- `ChunkMesh`：超大透明网格自动拆分为多个安全 part，每个 part 顶点数不超过 65536，索引不越界；普通方块 / 矿车 / 村民网格结构。
- `World.raycast`：命中实体方块、面朝向、最大距离、穿水等非实体方块。
- `World` 存档：异步保存等待、文件魔数与条目、编辑写入后重新加载。
- 区块编辑注入与编辑应用。

```bash
bash gradlew testDebugUnitTest
```

## 已知限制

- 只有 Android 客户端，无桌面或 iOS 版本。
- 单 part 仍使用 `GL_UNSIGNED_SHORT` 索引；超出 65535 顶点时通过多 part / 多 draw call 拆分，未使用 VAO 或索引扩展。
- 存档只记录玩家编辑过的方块，地形本身仍由种子重新生成。
- 暂停时最多等待 5 秒让存档完成；极端 I/O 卡顿下可能超时。
- 测试为 JVM 层逻辑测试，不包含 OpenGL 渲染、音频和触控 UI 的仪器测试。
- 世界为无限生成，长时间远距离探索后浮点精度和内存占用会逐渐增加。

## 截图

> 截图占位：城市与车站远景
>
> 截图占位：地下矿洞与矿车
>
> 截图占位：背包合成与设置面板
