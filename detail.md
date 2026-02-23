# 项目详情文档

本项目是一个基于 Spring Boot 和 DeepSeek 大模型驱动的人生模拟游戏后端服务。通过 AI 生成动态剧情、NPC 互动和随机事件，为用户提供沉浸式的人生体验。

## 1. 项目结构

项目采用标准的分层架构（Layered Architecture），目录结构如下：

```
src/main/java/com/example/demo/
├── controller/       # 控制层：处理 HTTP 请求，定义 API 接口
│   └── SimulationController.java
├── service/          # 服务层：核心业务逻辑实现
│   ├── SimulationService.java  # 模拟主流程控制
│   ├── DeepSeekService.java    # AI 接口调用封装
│   ├── PromptService.java      # 提示词工程管理
│   ├── NPCService.java         # NPC 行为演化
│   ├── PersistenceService.java # 存档/读档逻辑
│   └── UserService.java        # 用户账户管理
├── engine/           # 引擎层：游戏规则与随机性计算
│   ├── DestinyEngine.java      # 命运/随机事件判定
│   └── WorldContext.java       # 宏观世界背景生成
├── model/            # 模型层：数据实体定义
│   ├── UserProfile.java        # 用户核心档案（状态、属性、关系）
│   ├── GameInstance.java       # 游戏实例会话
│   ├── NPC.java                # 非玩家角色实体
│   ├── LifeHistory.java        # 人生履历记录
│   ├── CharacterTemplate.java  # 角色模板
│   ├── Checkpoint.java         # 存档快照
│   └── User.java               # 用户账户实体
├── repository/       # 数据访问层：JPA 接口定义
│   ├── UserProfileRepository.java
│   ├── GameInstanceRepository.java
│   └── ... (其他实体对应的 Repository)
└── DemoApplication.java # 启动入口
```

## 2. 项目逻辑

核心游戏循环围绕“年度模拟”展开，通过 `SimulationService.simulateYear` 方法驱动：

1.  **宏观事件生成 (Macro Event)**: 调用 `WorldContext` 生成当年的社会/历史大背景。
2.  **随机事件判定 (Destiny Check)**: `DestinyEngine` 根据概率触发随机事件（如“黑天鹅”、“小确幸”），并结合用户上一步的选择判定结果。
3.  **NPC 演化 (NPC Evolution)**: `NPCService` 更新所有相关 NPC 的状态（年龄、健康、亲密度），并生成 NPC 的动态生活状况。
4.  **AI 叙事生成 (Narrative Generation)**: `PromptService` 组装包含所有上下文（档案、事件、NPC状态）的提示词，调用 `DeepSeekService` 生成剧情文本。
5.  **状态更新 (State Update)**: 更新用户档案的年龄、精力值、人生履历。
6.  **选项生成 (Choice Generation)**: 根据当前剧情，AI 动态生成 3-4 个下一步的行动选项。
7.  **记忆压缩 (Memory Consolidation)**: 将本年度的重要经历压缩存入长期记忆字段，供后续 AI 参考。

## 3. 项目模块

*   **用户模块 (User Module)**: 处理用户注册、登录（基于手机号），以及用户关联的游戏历史查询。
*   **模拟核心模块 (Simulation Core)**:
    *   **时间系统**: 按“年”推进，控制年龄增长。
    *   **属性系统**: 管理健康、经济、家庭、社交等多维属性。
    *   **事件系统**: 结合随机性与 AI 创作的事件生成。
*   **AI 模块 (AI Integration)**:
    *   **DeepSeekService**: 封装 HTTP 调用，包含指数退避重试机制、JSON 格式清洗与校验。
    *   **PromptService**: 管理不同场景（开场、年度、结局、NPC互动）的提示词模板。
*   **持久化模块 (Persistence Module)**:
    *   利用 JPA/Hibernate 自动映射实体到数据库表。
    *   提供存档点（Checkpoint）的创建与回溯功能。

## 4. 项目接口 (API Endpoints)

所有接口位于 `/api/v1` 路径下：

### 用户相关
*   `POST /user/login`: 用户登录/注册 (参数: phone)
*   `GET /user/{id}`: 获取用户信息

### 游戏管理
*   `POST /game/start`: 开始新游戏 (参数: userId, templateId, profile)
*   `GET /game/history`: 获取用户游戏历史
*   `GET /game/{instanceId}`: 获取特定游戏实例详情

### 模拟流程
*   `POST /simulation/init`: 初始化档案 (生成属性、NPC)
*   `POST /simulation/probes`: 生成性格测试问题
*   `POST /simulation/analyze`: 分析测试结果并启动游戏
*   `POST /simulation/year`: 模拟下一年 (参数: profileId, choice)
*   `POST /simulation/skip`: 跳过多年 (参数: profileId, years)

### 存档/模板
*   `POST /simulation/template/create`: 创建角色模板
*   `GET /simulation/templates`: 获取用户模板列表
*   `POST /simulation/checkpoint/create`: 创建存档点
*   `POST /simulation/checkpoint/load`: 加载存档点

## 5. 数据流转

以“模拟一年”为例的数据流向：

1.  **Client (前端)** 发送 `POST /simulation/year` 请求，携带 `profileId` 和 `userChoice`。
2.  **Controller** 接收请求，调用 `SimulationService.simulateYear`。
3.  **Service** 从 **Repository** (`UserProfileRepository`) 加载 `UserProfile` 实体。
4.  **Service** 依次调用 **Engine** 组件：
    *   `WorldContext` -> 获取宏观事件字符串。
    *   `DestinyEngine` -> 获取随机事件类型和结果判定。
    *   `NPCService` -> 更新 `UserProfile` 中的 `npcs` 列表。
5.  **Service** 调用 **PromptService** 构建 Prompt。
6.  **Service** 调用 **DeepSeekService** 发送请求给大模型 API。
7.  **DeepSeekService** 接收 JSON 响应，校验格式并返回。
8.  **Service** 解析响应，更新 `UserProfile` 的 `currentScenario`, `lifeHistory`, `availableChoices` 等字段。
9.  **Service** 通过 **Repository** 保存更新后的 `UserProfile` 到数据库。
10. **Controller** 将更新后的 `UserProfile` 返回给 **Client** 渲染。

---
**注**：本项目所有核心业务代码（Controller, Service, Model, Engine, Repository）均已包含完整的中文注释，便于开发者理解和维护。
