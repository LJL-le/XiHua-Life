# xhul-ife / 西华生活服务平台

这是从原 nginx 静态资源恢复并做展示层改造后的前端目录。接口路径、请求参数、token/sessionStorage、axios 拦截器逻辑保持原样。

## 启动

使用 Node 预览并代理后端：

```powershell
cd D:\develop\Program\xhu-life\mul1
node dev-server.js
```

访问：http://localhost:5173

或用 nginx，把本目录作为 root，并使用本目录的 nginx.conf，把 /api 代理到 http://127.0.0.1:8081。
