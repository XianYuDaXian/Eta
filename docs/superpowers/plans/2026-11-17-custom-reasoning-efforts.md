# 自定义推理强度实施计划

> **给执行代理：** 必须按任务逐项执行，使用复选框跟踪进度。

**目标：** 让 Eta 支持任意原始字符串作为推理强度，并把自定义值同步到每个模型的候选列表，位置可设为最前或最后。

**架构：** 先把封闭枚举改为开放值类型，再由偏好层保存自定义值和位置；模型能力、设置页、聊天栏和请求编码统一读取合并后的候选列表。逐模型启用状态保持不变，自定义值只增加可选项。

**技术栈：** Kotlin、Jetpack Compose、SharedPreferences、kotlinx.serialization、JUnit。

**规格：** docs/superpowers/specs/2026-11-17-custom-reasoning-efforts-design.md

## 全局约束

- 包名保持 io.github.mangi.eta。
- 默认位置为 last，可切换为 first。
- 自定义值为非空且不含空白的字符串，保留输入顺序。
- 新增后不自动替任何模型勾选；旧会话继续按原字符串读取。
- 不修改 Codex 叠加层提示词和开关逻辑。
- 执行方式为本机直接实现，不启用子代理。

## 审查重点

- 自定义值含大写、连字符、点时，必须原样保存和发送，不能强制小写或规范化。
- 删除自定义值后，历史会话不能丢失或退回默认值。
- 位置切换后，模型设置列表、聊天栏菜单和默认强度列表必须同时更新。
- 内置 off/default 的语义不能因开放值类型而改变。
- 需要数值预算的供应商收到未知自定义值时必须给出明确错误，不能静默映射成其他强度。

---

### 任务 1：开放推理强度值类型

**文件：**
- 修改：app/src/main/kotlin/io/github/mangi/eta/data/model/Reasoning.kt
- 测试：app/src/test/kotlin/io/github/mangi/eta/data/model/ReasoningEffortTest.kt

**接口：**
- 产出：ReasoningEffort(rawValue: String)、ReasoningEffort.OFF/DEFAULT/MINIMAL/LOW/MEDIUM/HIGH/XHIGH/MAX、ReasoningEffort.knownValues、ReasoningEffort.fromWireValue、ReasoningEffortSerializer

- [ ] **步骤 1：编写失败测试**

验证任意原始值可被解析、内置别名仍可解析、未知自定义值的显示名为原值、序列化往返保持原值。

- [ ] **步骤 2：运行测试并确认失败**

运行 ./gradlew :app:testDebugUnitTest --tests '*ReasoningEffortTest*'。

- [ ] **步骤 3：实现开放值类型**

将枚举改为带自定义序列化器的值类型，保留全部既有常量；OFF 保持关闭语义。

- [ ] **步骤 4：运行测试并确认通过**

运行同一测试命令，结果必须为 PASS。

- [ ] **步骤 5：提交**

提交信息：feat: 开放推理强度原始值类型。

### 任务 2：保存自定义列表与位置

**文件：**
- 修改：app/src/main/kotlin/io/github/mangi/eta/config/Prefs.kt
- 新建：app/src/main/kotlin/io/github/mangi/eta/config/CustomReasoningEfforts.kt
- 测试：app/src/test/kotlin/io/github/mangi/eta/config/CustomReasoningEffortsTest.kt

**接口：**
- 产出：Prefs.Keys.AGENT_CUSTOM_REASONING_EFFORTS、Prefs.Keys.AGENT_CUSTOM_REASONING_EFFORT_POSITION、Prefs.customReasoningEfforts、Prefs.setCustomReasoningEfforts、Prefs.customReasoningEffortPosition、CustomReasoningEfforts.merge

- [ ] **步骤 1：编写失败测试**

验证去重、空白过滤、大小写保留、first 与 last 两种合并顺序。

- [ ] **步骤 2：运行测试并确认失败**

运行 ./gradlew :app:testDebugUnitTest --tests '*CustomReasoningEffortsTest*'。

- [ ] **步骤 3：实现偏好读写与合并函数**

用 JSON 数组保存有序原始值，位置字符串只接受 first/last。

- [ ] **步骤 4：运行测试并确认通过**

运行同一测试命令，结果必须为 PASS。

- [ ] **步骤 5：提交**

提交信息：feat: 保存自定义推理强度配置。

### 任务 3：同步模型候选列表与请求编码

**文件：**
- 修改：app/src/main/kotlin/io/github/mangi/eta/data/model/Reasoning.kt
- 修改：app/src/main/kotlin/io/github/mangi/eta/data/provider/ReasoningCapabilityResolver.kt
- 修改：app/src/main/kotlin/io/github/mangi/eta/data/repository/RemoteModelFetcher.kt
- 修改：app/src/main/kotlin/io/github/mangi/eta/agent/model/ProviderReasoning.kt
- 修改：app/src/main/kotlin/io/github/mangi/eta/config/Prefs.kt
- 测试：更新相关现有测试

**接口：**
- 消费：CustomReasoningEfforts.merge
- 产出：ModelReasoningCapabilities.selectableEfforts 包含已保存自定义值；请求编码对命名强度写入原始值。

- [ ] **步骤 1：编写失败测试**

验证模型候选包含自定义值、位置生效、OpenAI 兼容请求中的 reasoning_effort 原样发送、预算型供应商拒绝未知值。

- [ ] **步骤 2：运行测试并确认失败**

运行 ./gradlew :app:testDebugUnitTest --tests '*Reasoning*' --tests '*ProviderReasoning*'。

- [ ] **步骤 3：实现候选合并与编码分支**

为所有穷举 when 增加自定义分支；命名字段使用 wireValue，预算字段继续使用已有映射。

- [ ] **步骤 4：运行测试并确认通过**

运行同一测试命令，结果必须为 PASS。

- [ ] **步骤 5：提交**

提交信息：feat: 同步自定义推理强度到模型能力。

### 任务 4：新增设置界面

**文件：**
- 修改：app/src/main/kotlin/io/github/mangi/eta/ui/SettingsScreen.kt
- 修改：app/src/main/kotlin/io/github/mangi/eta/ui/pages/providers/ProviderModelsTab.kt

**接口：**
- 消费：Prefs.customReasoningEfforts、Prefs.setCustomReasoningEfforts、CustomReasoningEfforts.merge

- [ ] **步骤 1：实现自定义强度管理入口**

在上下文扩展区域新增入口，支持输入原始值、删除、切换“放最前/放最后”，中文文案直接写在 Compose 中。

- [ ] **步骤 2：实现候选列表刷新**

模型设置页读取合并后的候选列表，切换位置后下一帧刷新。

- [ ] **步骤 3：执行静态检查**

运行 ./gradlew :app:lintDebug，必须无新增错误。

- [ ] **步骤 4：提交**

提交信息：feat: 添加自定义推理强度设置入口。

### 任务 5：全量验证

- [ ] **步骤 1：运行完整单元测试**

运行 ./gradlew :app:testDebugUnitTest，结果必须为 PASS。

- [ ] **步骤 2：构建 Debug APK**

运行 ./gradlew :app:assembleDebug，产物必须保留原包名。

- [ ] **步骤 3：核对提示词开关**

确认 agent_codex_overlay_enabled 默认值与注入条件未改变。

- [ ] **步骤 4：提交验证修正**

如有修正，提交信息：test: 完善自定义推理强度验证。
