# 湖北大学活动报名系统（hubu-activities）

> 基于 Spring Boot 的校园活动发布、报名、审核、签到一体化平台，内置智谱 GLM 大模型活动问答助手（支持 SSE 流式输出）。

---

## 一、项目简介

本项目面向高校学生与活动管理员，提供完整的校园活动生命周期管理：

- 学生端：浏览活动、报名/取消报名、查看已报名活动、签到、查询个人诚信分与学时
- 管理端：发布/编辑/删除活动、审核报名（单个/批量）、生成签到码、数据看板统计
- 智能助手：基于当前活动数据，用自然语言回答"最近有什么活动"这类问题，支持逐字流式输出

---

## 二、技术栈

| 分类 | 技术 | 版本 |
| --- | --- | --- |
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.6.13 |
| 安全 | Spring Security + JJWT | 0.11.5 |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis（Spring Data Redis） | — |
| 大模型 | 智谱 AI `zai-sdk`（glm-4-flash） | 0.3.5 |
| 工具 | Lombok、DevTools | — |
| 构建 | Maven | 3.6+ |

---

## 三、功能模块

| 模块 | 说明 |
| --- | --- |
| 认证 Auth | 注册、登录、Token 刷新、退出登录 |
| 活动 Activity | 列表、详情、按类型筛选、报名、取消报名、我的活动 |
| 审核 Review | 待审核活动列表、待审核学生、单个/批量审核 |
| 活动管理 Admin | 新增、编辑、删除、编辑回显 |
| 签到 Checkin | 生成二维码/数字签到码、用户签到、未签到扣分 |
| 用户 User | 个人信息、个人统计、修改密码、用户列表 |
| 数据看板 DataBoard | 报名趋势、分类统计、签到率 |
| AI 助手 Ai | 基于活动数据的问答，支持普通返回与 SSE 流式返回 |

---

## 四、项目结构

```
hubu-activities
├── pom.xml
└── src
    ├── main
    │   ├── java/cn/edu/hubu/hubuactivities
    │   │   ├── HubuActivitiesApplication.java    # 启动类
    │   │   ├── config
    │   │   │   ├── BusinessException.java        # 业务异常
    │   │   │   ├── CorsConfig.java               # 跨域配置
    │   │   │   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
    │   │   │   ├── MybatisPlusConfig.java        # 分页插件
    │   │   │   ├── RedisConfig.java              # Redis 序列化配置
    │   │   │   └── SecurityConfig.java           # 安全拦截规则
    │   │   ├── controller                        # 接口层
    │   │   │   ├── AuthController.java
    │   │   │   ├── ActivityController.java
    │   │   │   ├── UserController.java
    │   │   │   ├── DataBoardController.java
    │   │   │   └── AiController.java
    │   │   ├── service / service.impl            # 业务层
    │   │   ├── mapper                            # MyBatis-Plus Mapper（注解式 SQL）
    │   │   ├── entity                            # 数据库实体
    │   │   ├── vo                                # 出入参对象
    │   │   ├── utils                             # JwtUtils、PasswordUtils
    │   │   └── temp/GenerateTestData.java        # 测试数据生成工具
    │   └── resources
    │       ├── application.properties            # 配置
    │       └── static/index.html                 # 简易测试页
    └── test/java/...                             # 单元测试
```

---

## 五、快速开始

### 5.1 环境要求

- JDK 8
- MySQL 8.x
- Redis 6.x
- Maven 3.6+

### 5.2 数据库准备

创建数据库：

```sql
CREATE DATABASE activities_school DEFAULT CHARACTER SET utf8mb4;
```

核心数据表：

| 表名 | 实体类 | 说明 |
| --- | --- | --- |
| `student` | `Student` | 学生/用户信息（含诚信分、拉黑状态、角色） |
| `student_token` | `StudentToken` | 登录令牌记录（访问令牌 + 刷新令牌） |
| `activity` | `Activity` | 活动信息（时间、名额、积分、学时、签到配置） |
| `activity_type` | `ActivityType` | 活动分类（是否计学分/学时、取值范围） |
| `activity_sign` | `Sign` | 报名记录（待审核/已录取/未录取/已签到/已取消） |

> 项目未使用 XML 映射文件，SQL 以 `@Select` 等注解写在 `mapper` 接口中。

### 5.3 修改配置

编辑 [application.properties](file:///E:/ideaprojects/hubu-activities/src/main/resources/application.properties)：

```properties
server.port=8088
server.servlet.context-path=/ycw          # 所有接口前缀 /ycw

spring.datasource.url=jdbc:mysql://localhost/activities_school?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码

spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=你的Redis密码

jwt.secret=你的密钥（长度需 ≥ 32 字符）
jwt.expiration=7200000                    # 访问令牌有效期 2 小时
jwt.refreshExpiration=604800000           # 刷新令牌有效期 7 天

zhipu.api-key=你的智谱API Key
```

### 5.4 启动

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/hubu-activities-0.0.1-SNAPSHOT.jar
# 或直接运行 HubuActivitiesApplication
```

启动后访问：`http://localhost:8088/ycw`

---

## 六、接口文档

**统一前缀**：`http://localhost:8088/ycw`
**统一响应体**：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

`code` 约定：`200` 成功、`400` 参数错误、`401` 未认证/凭证错误、`403` 无权限/黑名单、`409` 资源冲突、`500` 服务异常。

**鉴权方式**：除登录/注册外，所有接口需在请求头携带：

```
Authorization: Bearer <token>
```

### 6.1 认证接口 `/auth`

| 方法 | 路径 | 说明 | 请求参数 |
| --- | --- | --- | --- |
| POST | `/auth/login` | 登录 | Body：`{ "stu_id": "20210001", "password": "123456" }` |
| POST | `/auth/register` | 注册 | Body：`stu_id`、`password`、`real_name`、`phone`、`school_name`、`department`、`major`、`political_status` |
| POST | `/auth/refresh` | 刷新令牌 | Header：`Refresh-Token: <refreshToken>` |
| POST | `/auth/logout` | 退出登录 | Header：`Authorization: Bearer <token>` |
| GET | `/auth/test` | 连通性测试 | — |

登录成功返回：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "xxx",
    "refreshToken": "yyy",
    "tokenType": "Bearer",
    "userInfo": {
      "id": 1, "stu_id": "20210001", "real_name": "张三",
      "role": "user", "credit": 100,
      "school_name": "湖北大学", "department": "计算机学院",
      "major": "软件工程", "phone": "138****0000"
    }
  }
}
```

### 6.2 活动接口 `/activity`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/activity/list` | 活动列表 |
| GET | `/activity/detail/{id}` | 活动详情 |
| GET | `/activity/type/{typeId}` | 按分类查询 |
| POST | `/activity/sign/{activityId}` | 报名活动 |
| GET | `/activity/check-signed/{activityId}` | 查询是否已报名 |
| POST | `/activity/cancel/{activityId}` | 取消报名 |
| GET | `/activity/my-activities` | 我报名的活动 |
| POST | `/activity/checkin?activityId=&checkinCode=` | 用户签到 |

管理员接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/activity/admin/pending-activities` | 有待审核报名的活动 |
| GET | `/activity/admin/pending-students/{activityId}` | 该活动待审核学生 |
| GET | `/activity/admin/all-students/{activityId}` | 该活动全部报名学生 |
| POST | `/activity/admin/review/{signId}?status=&remark=` | 审核单条报名 |
| POST | `/activity/admin/review/batch` | 批量审核，Body：`{ "signIds": [1,2], "status": "approved", "remark": "" }` |
| POST | `/activity/admin/create` | 新增活动，Body 为 `Activity` |
| PUT | `/activity/admin/update/{id}` | 编辑活动 |
| DELETE | `/activity/admin/delete/{id}` | 删除活动 |
| GET | `/activity/admin/detail/{id}` | 编辑回显 |
| POST | `/activity/admin/generate-checkin-code?activityId=&codeType=` | 生成签到码，`codeType` 取 `qr` / `num` |
| GET | `/activity/admin/checkin-info/{activityId}` | 查询签到码信息 |

报名状态流转（`activity_sign.status`）：

```
pending（待审核）──审核通过──> approved（已录取）──签到──> checked（已签到）
        │                          │
        └──审核拒绝──> rejected     └──取消报名──> cancelled
```

### 6.3 用户接口 `/user`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/user/stats` | 个人统计（姓名、总积分、总学时、诚信分） |
| GET | `/user/student-info` | 学生详细信息 |
| POST | `/user/change-password` | 修改密码，Body：`{ "oldPassword": "", "newPassword": "" }` |
| GET | `/user/admin/info` | 当前管理员信息 |
| GET | `/user/admin/stats` | 管理端总览统计 |
| GET | `/user/admin/users` | 全部用户列表 |

### 6.4 数据看板 `/admin`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/databoard` | 报名趋势、分类统计、总报名数、签到率 |

### 6.5 AI 助手 `/ai`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/ai/chat` | 普通问答，一次性返回完整答案 |
| POST | `/ai/chat/stream` | 流式问答，SSE 逐段返回 |
| GET | `/ai/clear` | 清空 AI 问答缓存（`ai:chat:*`） |

请求体统一为：

```json
{ "question": "最近有什么活动？" }
```

**流式接口返回格式**（`Content-Type: text/event-stream;charset=UTF-8`）：

```
data:{"content":"最近"}
data:{"content":"有以下活动"}
data:{"done":true}
```

异常时返回：

```
data:{"error":"抱歉，AI助手现在有点累，请稍后再试。"}
```

**前端消费示例（必须用 fetch，不能用 axios/EventSource）**：

```javascript
const res = await fetch('/ycw/ai/chat/stream', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  body: JSON.stringify({ question: '最近有什么活动？' })
});

const reader = res.body.getReader();
const decoder = new TextDecoder('utf-8');
let buffer = '';

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  buffer += decoder.decode(value, { stream: true });

  const events = buffer.split('\n\n');
  buffer = events.pop();
  for (const evt of events) {
    const line = evt.split('\n').find(l => l.startsWith('data:'));
    if (!line) continue;
    const payload = JSON.parse(line.slice(5).trim());
    if (payload.content) appendToBubble(payload.content);
  }
}
```

---

## 七、核心设计说明

### 7.1 JWT 双 Token 鉴权

1. 登录成功后签发 **访问令牌（2 小时）** 与 **刷新令牌（7 天）**，同时落库到 `student_token`
2. 再次登录会调用 `invalidateAllTokens` 让该用户旧令牌全部失效（单点登录效果）
3. `JwtAuthenticationFilter` 解析 `Authorization: Bearer <token>`，把 `stu_id` 放入 `SecurityContext`，业务层通过 `SecurityContextHolder` 取当前用户
4. 访问令牌过期后，用 `POST /auth/refresh`（Header 带刷新令牌）换取新令牌
5. 退出登录把记录标记 `is_valid = 0` 并写入 `logout_time`

### 7.2 签到码

- 管理员按活动生成 `qr`（二维码）或 `num`（数字码），写入 `activity.num_code` 等字段并设置有效期
- 学生调用 `/activity/checkin` 提交签到码，服务端校验有效期与学生报名状态后置为 `checked`
- 活动结束后可批量处理未签到学生，按 `credit_absent` 扣减诚信分

### 7.3 诚信分机制

- 新用户默认 `credit = 100`
- 报名后无故缺席按活动配置扣分；正常签到按 `credit_sign` 加分
- `is_black = 1` 的用户登录直接返回 403

### 7.4 AI 问答

- 每次提问先查 Redis 缓存，命中直接返回（流式接口命中时一次性推送全文）
- 未命中则把数据库中全部活动拼接进 System Prompt，交给 `glm-4-flash` 回答
- 缓存 Key 为 `ai:chat:` + 问题 MD5，过期时间 1 小时 + 0~5 分钟随机抖动，避免缓存雪崩
- 流式模式下用 `StringBuilder` 累积全文，**生成完毕后**才回写缓存，避免半截答案被缓存
- Redis 异常时降级为直接调用大模型，不影响主流程

---

## 八、常见问题

**1. Redis 连接失败**

检查 Redis 是否启动、`spring.redis.password` 是否与本地一致；应用已做降级处理，Redis 不可用时 AI 功能仍可工作。

**2. 接口返回 401**

确认请求头格式为 `Authorization: Bearer xxx`（注意 Bearer 后有空格），且 token 未过期。

**3. 跨域失败**

`CorsConfig` 已放开跨域；若前端使用 `credentials`，需保证 `allowedOriginPatterns` 为具体域名而非 `*`。

**4. 流式输出"一次性全部显示"**

- 前端必须用 `fetch` + `res.body.getReader()`，axios 会等响应体完整才回调
- 若部署在 Nginx 后面，需加 `proxy_buffering off;` 和 `proxy_read_timeout 300s;`，否则 SSE 会被缓冲

**5. IDE 报 `Language Support for Java client: couldn't create connection to server. Error: spawn UNKNOWN`**

Java 扩展（redhat.java 1.59+）要求用 **JDK 25 及以上** 启动语言服务器，且扩展自带的是 ARM64 版 JRE，在 x64 机器上无法执行。解决办法：安装 x64 的 JDK 25，并在 Trae 用户 `settings.json` 中指定：

```json
{ "java.jdt.ls.java.home": "D:\\Eclipse Adoptium\\jdk-25.x.x" }
```

然后执行"重载窗口"。

---

## 九、待优化项

| 项 | 说明 |
| --- | --- |
| 密码明文存储 | 当前登录/注册直接比较明文，`PasswordUtils` 尚未接入，建议改为 BCrypt |
| 审核人写死 | `ActivityController` 中 `reviewerId` 固定为 `1L`，应改为从登录用户获取 |
| 敏感配置外置 | 数据库密码、Redis 密码、智谱 API Key 目前写在 `application.properties`，建议改用环境变量或配置中心 |
| 令牌校验 | 仅做 JWT 自校验，未校验数据库 `is_valid`，登出后旧令牌在有效期内仍可用 |
