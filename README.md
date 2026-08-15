# XHU Life

西华校园生活服务课程演示项目，包含商铺、探店笔记、关注评论、商铺评价、站内消息、限量免单和简易管理台。

## 环境要求

- Java 8
- Maven 3.6+
- MySQL 8
- Redis 7
- Node.js

数据库结构及演示数据位于 `sql/init.sql`。如果使用已有的旧数据库，请按需执行 `sql/upgrade-existing.sql`。

## 启动后端

先启动已有的 MySQL 和 Redis，然后设置与实际环境一致的连接参数：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/xhulife?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的MySQL密码"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="16379"
$env:REDIS_PASSWORD="你的Redis密码"
$env:APP_DEV_MODE="true"
```

然后通过 IDEA 运行 `XhuLifeApplication`，或执行：

```powershell
mvn spring-boot:run
```

项目默认后端端口为 `8081`。如果该端口被 Windows 保留，可设置：

```powershell
$env:SERVER_PORT="8500"
```

## 启动前端

如果后端运行在默认的 `8081`：

```powershell
cd D:\develop\Program\xhu-life\mul1
node dev-server.js
```

如果后端运行在 `8500`：

```powershell
cd D:\develop\Program\xhu-life\mul1
$env:BACKEND_PORT="8500"
node dev-server.js
```

访问地址：

- 用户端：<http://localhost:5173>
- 管理端：<http://localhost:5173/admin/>

演示账号：

- 管理员：`13800000000 / admin123`
- 普通用户：`13900000000 / user123`

开发模式发送验证码时会直接在页面提示验证码；关闭 `APP_DEV_MODE` 后接口不会返回验证码。

## 测试

```powershell
mvn test
```

上传图片限制为 5 MB，支持 jpg、jpeg、png 和 webp，并按登录用户隔离存储。
