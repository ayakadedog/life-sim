# DeepLife: AI 驱动的人生重开模拟器 (Life Simulator)

> "性格决定命运，而 AI 重构人生。"

DeepLife 是一个基于大语言模型（DeepSeek LLM）驱动的沉浸式文本人生模拟游戏。不同于传统的数值堆砌类游戏，DeepLife 利用 LLM 强大的叙事能力，根据玩家的初始设定（性格、家庭、天赋）和每一次的关键抉择，实时生成独一无二的人生剧本。

## 🌟 核心特性

- **🧠 AI 驱动叙事**
  集成 DeepSeek API，摒弃死板的预设事件库。每一次人生经历都是 AI 根据当前上下文实时创作的，保证了无限的可重玩性。

- **🎭 多维角色构建**
  - **基础属性**：年龄、学历、职业、居住地。
  - **大五人格**：开放性、尽责性、外向性、宜人性、神经质，深度影响 AI 的剧情走向。
  - **软性资产**：家庭背景、人际关系、健康状况（精力值）。

- **🔮 拟真模拟系统**
  - **心理探针 (Psychological Probes)**：开局通过 AI 生成的心理测试题，定格玩家的深层价值观。
  - **时光流转**：支持逐年模拟和“快进”模式（跳过平淡年份）。
  - **随机事件**：从“小确幸”到“黑天鹅”，生活充满了不确定性。
  - **主动抉择**：面对人生岔路口，输入你的想法，AI 会根据你的决策重塑未来的轨迹。

- **🧬 代际传承 (Legacy System)**
  人生苦短，但家族长存。当你的一生结束时，可以选择将资产和意志传承给下一代，开启“二周目”人生。

- **📊 数据可视化**
  前端采用 Vue 3 + Tailwind CSS 构建，提供清晰的角色卡片、属性面板和即时反馈的打字机叙事效果。

## 📘 详细文档

项目包含完整的架构设计、数据流转和接口说明，请参考：
👉 [项目详细设计文档 (detail.md)](detail.md)

> **注**：本项目所有核心业务代码（Controller, Service, Model, Engine, Repository）均已包含完整的中文注释，便于开发者理解和维护。

## 🛠 技术栈

- **后端**
  - Java 17+
  - Spring Boot 3.2.x
  - Spring Data JPA (Hibernate)
  - MySQL 8.x
  - Lombok

- **前端**
  - HTML5 / CSS3
  - Vue.js 3 (Composition API)
  - Tailwind CSS (CDN)

- **AI 服务**
  - DeepSeek API (Chat Completion)

## 🚀 快速开始

### 前置要求
- JDK 17 或更高版本
- Maven 3.6+
- MySQL 数据库

### 1. 克隆项目
```bash
git clone https://github.com/your-username/deep-life-simulator.git
cd deep-life-simulator
```

### 2. 配置数据库与 API Key
修改 `src/main/resources/application.properties` 文件：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/life_sim?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password

# DeepSeek API 配置 (请务必替换为你自己的 Key)
deepseek.api.key=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
deepseek.api.url=https://api.deepseek.com/chat/completions

# 服务端口
server.port=8081
```

> ⚠️ **注意**：请勿将包含真实 API Key 的配置文件提交到公共仓库。

### 3. 运行项目
```bash
mvn spring-boot:run
```
或者在 IDE (IntelliJ IDEA / Eclipse) 中直接运行 `DemoApplication.java`。

### 4. 访问应用
打开浏览器访问：`http://localhost:8081`

## 📂 项目结构

```
src/main
├── java/com/example/demo
│   ├── controller      # REST API 控制器
│   ├── engine          # 核心模拟引擎 (Prompt 构建、状态管理)
│   ├── model           # 实体类 (User, UserProfile, GameInstance)
│   ├── repository      # 数据访问层 (JPA Repository)
│   ├── service         # 业务逻辑 (Simulation, DeepSeek integration)
│   └── DemoApplication.java
└── resources
    ├── static          # 前端资源 (HTML, JS)
    └── application.properties
```

## 📝 待办事项 / 路线图

- [ ] **成就系统**：记录人生的里程碑。
- [ ] **社交网络**：更复杂的 NPC 交互系统。
- [ ] **多模态支持**：为关键事件生成配图。
- [ ] **排行榜**：比较不同玩家的人生评分。

## 🤝 贡献
欢迎提交 Issue 或 Pull Request！

## 📄 许可证
MIT License
