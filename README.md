# 在线教育测评系统

基于 Spring Boot + Vue 3 的在线教育测评系统，支持课程管理、题库管理、在线考试、AI 智能批改及成绩分析等功能。

## 技术栈

### 后端

| 技术 | 说明 |
|------|------|
| Java 17 | 编程语言 |
| Spring Boot 3.2.3 | 应用框架 |
| Spring Security + JWT | 认证授权 |
| Spring Data JPA | ORM 持久化 |
| MySQL 8.x | 数据库 |
| DeepSeek API | AI 批改 & 智能辅导 |
| EasyExcel / Apache POI | 题目批量导入 |
| Maven | 构建工具 |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 3 | 前端框架 (Composition API) |
| Vite | 构建工具 |
| Element Plus | UI 组件库 |
| Pinia | 状态管理 |
| Vue Router | 路由管理 |
| ECharts | 数据可视化 |

## 项目结构

```
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/example/onlineexam/
│   │   ├── controller/         # REST 控制器 (13个)
│   │   ├── service/            # 业务逻辑层
│   │   ├── model/              # JPA 实体类
│   │   ├── repository/         # 数据访问层
│   │   ├── payload/            # 请求/响应 DTO
│   │   ├── security/           # JWT 认证模块
│   │   ├── config/             # 安全与跨域配置
│   │   ├── annotation/         # 自定义注解 (@LogAction)
│   │   └── aspect/             # AOP 操作日志
│   └── src/main/resources/
│       └── application.properties
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── views/              # 页面组件 (26个)
│   │   ├── api/                # API 请求模块
│   │   ├── router/             # 路由配置
│   │   ├── store/              # Pinia 状态管理
│   │   └── assets/             # 静态资源
│   └── package.json
└── README.md
```

## 功能特性

### 角色权限

系统支持三种角色：**管理员**、**教师**、**学生**

| 功能 | 管理员 | 教师 | 学生 |
|------|:------:|:----:|:----:|
| 用户管理 | ✓ | - | - |
| 课程管理 | ✓ | ✓ | - |
| 题库管理 | ✓ | ✓ | - |
| 考试管理 | ✓ | ✓ | - |
| 在线考试 | - | - | ✓ |
| 成绩批改 | - | ✓ | - |
| 成绩查看 | ✓ | ✓ | ✓ |
| 操作日志 | ✓ | - | - |

### 核心功能

- **课程管理** — 课程增删改查、章节管理、文件上传（最大 10MB）
- **题库管理** — 支持单选、多选、判断、主观四种题型，支持 Excel/Word/TXT 批量导入
- **在线考试** — 倒计时、切屏检测（3 次警告）、单题/整卷两种模式
- **AI 智能批改** — 基于 DeepSeek API 的主观题自动评分与解析
- **AI 智能辅导** — 错题讲解，生成随机练习卷
- **成绩分析** — 成绩统计、错题回顾、成绩导出 Excel
- **操作日志** — 基于 AOP 的系统操作审计记录

## 快速开始

### 环境要求

- Java 17+
- MySQL 8.x
- Node.js 20.19+ 或 22.12+

### 后端启动

1. 创建数据库：

```sql
CREATE DATABASE online_exam DEFAULT CHARACTER SET utf8mb4;
```

2. 修改数据库配置（`backend/src/main/resources/application.properties`）：

```properties
spring.datasource.username=root
spring.datasource.password=你的MySQL密码
```

3. （可选）配置 DeepSeek API Key（`application-secret.properties`）：

```properties
deepseek.api.key=你的API密钥
```

4. 启动后端：

```bash
cd backend
./mvnw spring-boot:run
```

后端运行在 `http://localhost:8081`，首次启动自动建表并初始化默认账号。

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:3000`，自动代理 API 请求到后端。

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 教师 | teacher | 123456 |
| 学生 | student | 123456 |

### 生产构建

```bash
cd frontend
npm run build
```

构建产物输出到 `frontend/dist/` 目录。

## 许可证

本项目仅供学习交流使用。
