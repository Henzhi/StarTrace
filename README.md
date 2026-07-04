# 🌟 StarTrace 星迹

> 捕捉瞬间灵感，化作星辰碎片 ✨

[English](#english) | [中文](#中文)

***

## English

### 🌟 Project Overview

StarTrace is a creative writing application that captures fleeting inspirations and transforms them into star fragments. With the power of AI, these fragments can be woven into complete stories.

### ✨ Features

- **Inspiration Capture** - Record creative fragments with domain tags and mood indicators
- **AI Story Generation** - Generate complete stories from selected fragments using SSE streaming
- **Galaxy View** - Visualize your creative universe with force-directed graph layout
- **Story Library** - Organize and read all your generated stories
- **Profile Management** - Local user system with avatar upload support
- **LLM Configuration** - Configure AI model parameters for story generation

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

### 🚀 Quick Start

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
- GitHub: [MaHuHu-Dev](https://github.com/MaHuHu-Dev)

***

## 中文

### 🌟 项目介绍

星迹（StarTrace）是一款创意写作应用，帮助用户捕捉瞬间灵感，将其化作星辰碎片。借助 AI 的力量，这些碎片可以编织成完整的故事。

### ✨ 功能特性

- **灵感记录** - 使用领域标签和心情指示器记录创意碎片
- **AI 故事生成** - 使用 SSE 流式生成从选中的碎片生成完整故事
- **星系视图** - 使用力导向图可视化你的创意宇宙
- **故事库** - 组织和阅读所有生成的故事
- **个人管理** - 本地用户系统，支持头像上传
- **LLM 配置** - 配置 AI 模型参数以生成故事

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

### 🚀 快速开始

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

