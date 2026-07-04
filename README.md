# 🌟 StarTrace 星迹

> 捕捉瞬间灵感，化作星辰碎片 ✨

[![Offline Edition](https://img.shields.io/badge/Version-0.1.0%20Offline-blue.svg)](https://github.com/MaHuHu-Dev/StarTrace/releases/tag/v0.1.0)

[English](#english) | [中文](#中文)

***

## English

### 🌟 What is StarTrace?

StarTrace is a creative writing companion that helps you capture fleeting inspirations and weave them into complete stories. Designed as a **fully offline first** application, all your data stays securely on your device.

**Important Note for v0.1.0:** This is the **completely offline edition**. Your fragments, stories, and user profile are stored locally on your device. Story generation via AI requires you to configure your own LLM API endpoint in the settings.

### 📥 Download & Install

#### Prerequisites

- Android 8.0 (API Level 26) or higher
- Approximately 30MB of storage space

#### Installation Steps

1. **Download the APK**
   - Go to [GitHub Releases](https://github.com/MaHuHu-Dev/StarTrace/releases)
   - Download the latest `StarTrace-v0.1.0.apk` file
2. **Enable Unknown Sources**
   - Open **Settings** → **Security** (or **Privacy**)
   - Enable **Install unknown apps**
   - Select your browser/file manager and allow installations
3. **Install the APK**
   - Open the downloaded APK file
   - Tap **Install** and wait for completion
   - Tap **Open** to launch StarTrace

### 📖 User Guide (Quick Start)

#### 1. First Launch

- When you open StarTrace for the first time, a local user account is automatically created
- No registration required, no internet connection needed
- Your data is tied to your device and stored locally

#### 2. Record Inspiration Fragments

1. Tap the **+** button at the bottom navigation bar
2. Enter your inspiration content in the text area
3. Add **domain tags** (e.g., fantasy, sci-fi, romance)
4. Select a **mood indicator** to set the emotional tone
5. Tap **Save** to store your fragment

#### 3. Explore the Galaxy

1. Tap the **Galaxy** icon in the bottom navigation
2. View your fragments as stars in a force-directed graph
3. Tap any star to view its details
4. Long-press and drag stars to rearrange them

#### 4. Configure LLM for Story Generation

1. Go to **Profile** → **LLM Configuration**
2. Enter your AI model API endpoint URL
3. Configure model parameters (temperature, max tokens)
4. Save your configuration

#### 5. Generate Stories (星辰编织)

1. Go to the **Story Generator** screen
2. Select one or more inspiration fragments
3. Tap **Generate Story** to start the AI generation
4. Watch the story unfold in real-time (SSE streaming)
5. Save your story to the library when complete

#### 6. Read and Manage Stories

1. Tap the **Library** icon in the bottom navigation
2. Browse your generated stories
3. Tap any story to read it
4. Swipe to delete unwanted stories

#### 7. Personalize Your Profile

1. Tap the **Profile** icon in the bottom navigation
2. **Change Avatar**: Tap your avatar to upload a local image
3. **View Statistics**: See your fragment count and story count
4. **Feedback**: Send suggestions via email
5. **About**: View app version information

### ✨ Features

- **Inspiration Capture** - Record creative fragments with tags and mood indicators
- **Galaxy View** - Visualize your creative universe with interactive force-directed graph
- **AI Story Generation** - Generate complete stories from fragments using SSE streaming
- **Story Library** - Organize and read all your generated stories
- **Profile Management** - Local user system with avatar upload support
- **LLM Configuration** - Configure AI model parameters for story generation
- **100% Offline** - All data stored locally, no server required

### 🛠️ Tech Stack

**Android**

- Kotlin 1.9+
- Jetpack Compose 2024.09.03
- Hilt 2.51.1 (Dependency Injection)
- Room 2.6.1 (Local Database)
- Retrofit 2.11.0 (Network)
- Coil 2.7.0 (Image Loading)

**Backend**

- Spring Boot 3.3.4
- Java 21
- PostgreSQL 16
- Redis 7
- MyBatis Plus
- Docker Compose

### 🚀 Development Setup

#### Android Development

```bash
# Clone the repository
git clone https://github.com/MaHuHu-Dev/StarTrace.git
cd StarTrace/android

# Build and run
./gradlew assembleDebug
```

**Requirements:**

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35

#### Backend Docker Deployment

```bash
cd StarTrace/backend

# Create environment file
cp .env.example .env

# Start all services
docker-compose up -d

# Verify health
curl http://localhost:8080/actuator/health
```

**Services:**

- API: <http://localhost:8080>
- Swagger: <http://localhost:8080/swagger-ui.html>
- PostgreSQL: localhost:5432
- Redis: localhost:6379

### 📁 Project Structure

```
StarTrace/
├── android/                    # Android Application
│   ├── app/
│   │   ├── src/main/java/com/startrace/
│   │   │   ├── core/           # Core modules (database, network, engine)
│   │   │   ├── design/         # Design system (components, themes)
│   │   │   ├── feature/        # Feature modules (fragment, story, galaxy, profile)
│   │   │   ├── ui/             # UI components (navigation)
│   │   │   ├── MainActivity.kt
│   │   │   └── StarTraceApplication.kt
│   │   └── src/main/res/       # Resources
│   └── gradle/                 # Gradle configuration
├── backend/                    # Spring Boot Backend
│   ├── src/main/java/com/startrace/api/
│   │   ├── controller/         # REST controllers
│   │   ├── service/            # Business logic
│   │   ├── mapper/             # Data access
│   │   ├── entity/             # Database entities
│   │   └── config/             # Configuration
│   ├── nginx/                  # Nginx reverse proxy
│   └── docker-compose.yml      # Docker deployment
├── .github/workflows/          # CI/CD pipelines
└── README.md
```

### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### 📧 Contact

- Email: <startrace2026@163.com>

***

## 中文

### 🌟 什么是星迹？

星迹（StarTrace）是一款创意写作伴侣，帮助你捕捉瞬间灵感，并将其编织成完整的故事。作为一款**完全离线优先**的应用，你所有的数据都安全地存储在设备本地。

**v0.1.0 重要说明：** 这是**完全离线版本**。你的碎片、故事和用户资料都存储在设备本地。通过 AI 生成故事需要你在设置中配置自己的 LLM API 端点。

### 📥 下载与安装

#### 系统要求

- Android 8.0（API Level 26）或更高版本
- 约 30MB 存储空间

#### 安装步骤

1. **下载 APK**
   - 访问 [GitHub Releases](https://github.com/MaHuHu-Dev/StarTrace/releases)
   - 下载最新的 `StarTrace-v0.1.0.apk` 文件
2. **允许未知来源安装**
   - 打开 **设置** → **安全**（或 **隐私**）
   - 开启 **允许安装未知来源应用**
   - 选择你的浏览器/文件管理器并允许安装
3. **安装 APK**
   - 打开下载的 APK 文件
   - 点击 **安装**，等待完成
   - 点击 **打开** 启动星迹

### 📖 操作手册（快速入门）

#### 1. 首次启动

- 首次打开星迹时，会自动创建一个本地用户账户
- 无需注册，无需互联网连接
- 你的数据与设备绑定，存储在本地

#### 2. 记录灵感碎片

1. 点击底部导航栏的 **+** 按钮
2. 在文本区域输入你的灵感内容
3. 添加**领域标签**（如：奇幻、科幻、言情）
4. 选择**心情指示器**来设置情感基调
5. 点击**保存**存储你的碎片

#### 3. 探索星系

1. 点击底部导航栏的**星系**图标
2. 查看你的碎片在力导向图中如同星辰般呈现
3. 点击任意星辰查看详情
4. 长按拖动星辰进行重新排列

#### 4. 配置 LLM 故事生成

1. 进入 **个人** → **LLM 配置**
2. 输入你的 AI 模型 API 端点 URL
3. 配置模型参数（温度、最大 tokens）
4. 保存配置

#### 5. 生成故事（星辰编织）

1. 进入**星辰编织**页面
2. 选择一个或多个灵感碎片
3. 点击**生成故事**开始 AI 生成
4. 实时观看故事展开（SSE 流式传输）
5. 完成后保存故事到故事库

#### 6. 阅读与管理故事

1. 点击底部导航栏的**故事库**图标
2. 浏览你生成的所有故事
3. 点击任意故事进行阅读
4. 滑动删除不需要的故事

#### 7. 个性化你的个人主页

1. 点击底部导航栏的**个人**图标
2. **更换头像**：点击头像上传本地图片
3. **查看统计**：查看你的碎片数量和故事数量
4. **意见反馈**：通过邮箱发送建议
5. **关于星迹**：查看应用版本信息

### ✨ 功能特性

- **灵感记录** - 使用标签和心情指示器记录创意碎片
- **星系视图** - 用交互式力导向图可视化你的创意宇宙
- **AI 故事生成** - 使用 SSE 流式传输从碎片生成完整故事
- **故事库** - 组织和阅读所有生成的故事
- **个人管理** - 本地用户系统，支持头像上传
- **LLM 配置** - 配置 AI 模型参数以生成故事
- **100% 离线** - 所有数据本地存储，无需服务器

### 🛠️ 技术栈

**Android**

- Kotlin 1.9+
- Jetpack Compose 2024.09.03
- Hilt 2.51.1（依赖注入）
- Room 2.6.1（本地数据库）
- Retrofit 2.11.0（网络请求）
- Coil 2.7.0（图片加载）

**后端**

- Spring Boot 3.3.4
- Java 21
- PostgreSQL 16
- Redis 7
- MyBatis Plus
- Docker Compose

### 🚀 开发环境搭建

#### Android 开发

```bash
# 克隆仓库
git clone https://github.com/MaHuHu-Dev/StarTrace.git
cd StarTrace/android

# 构建运行
./gradlew assembleDebug
```

**环境要求:**

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 35

#### 后端 Docker 部署

```bash
cd StarTrace/backend

# 创建环境配置文件
cp .env.example .env

# 启动所有服务
docker-compose up -d

# 验证服务健康状态
curl http://localhost:8080/actuator/health
```

**服务地址:**

- API: <http://localhost:8080>
- Swagger: <http://localhost:8080/swagger-ui.html>
- PostgreSQL: localhost:5432
- Redis: localhost:6379

### 📁 项目结构

```
StarTrace/
├── android/                    # Android 应用
│   ├── app/
│   │   ├── src/main/java/com/startrace/
│   │   │   ├── core/           # 核心模块（数据库、网络、引擎）
│   │   │   ├── design/         # 设计系统（组件、主题）
│   │   │   ├── feature/        # 功能模块（碎片、故事、星系、个人）
│   │   │   ├── ui/             # UI 组件（导航）
│   │   │   ├── MainActivity.kt
│   │   │   └── StarTraceApplication.kt
│   │   └── src/main/res/       # 资源文件
│   └── gradle/                 # Gradle 配置
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/startrace/api/
│   │   ├── controller/         # REST 控制器
│   │   ├── service/            # 业务逻辑
│   │   ├── mapper/             # 数据访问层
│   │   ├── entity/             # 数据库实体
│   │   └── config/             # 配置类
│   ├── nginx/                  # Nginx 反向代理
│   └── docker-compose.yml      # Docker 部署配置
├── .github/workflows/          # CI/CD 流水线
└── README.md
```

### 📄 开源协议

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

### 📧 联系方式

- 邮箱: <startrace2026@163.com>

