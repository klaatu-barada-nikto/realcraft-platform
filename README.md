# realcraft-platform

像素三视图转 Minecraft 方块的极简一体化服务（Go 后端 + Vue 前端）。

## 项目简介

用户上传已像素化的物体三视图（正视、侧视、俯视），服务调用多模态 AI 进行空间推理，通过三视图投影交集运算输出确定性的 Minecraft 体素坐标，并以紧凑二维数组 JSON 文件对外分发，供 Fabric 模组通过 `/buildmodel <url>` 导入构建。

## 技术栈

- 后端：Go 1.22+，Gin
- 前端：Vue 3 + Element Plus + Vite（构建产物 `dist` 由后端统一托管）
- 存储：本地文件系统（`./data/images` 存原图、`./data/models` 存模型 JSON），无数据库
- 托管：单进程统一托管 API 路由、模型静态分发与前端 `dist` 静态资源（SPA 回退），无 Nginx

## 环境变量

服务所有配置均通过环境变量注入，命名遵循 `REALCRAFT_` 前缀 + UPPER_SNAKE_CASE。

| 变量 | 含义 | 默认值 | 必填 |
|------|------|--------|------|
| `REALCRAFT_SERVER_PORT` | 服务监听端口 | `8080` | 否 |
| `REALCRAFT_AI_API_URL` | AI 推理服务地址（OpenAI 兼容，不含 `/chat/completions`，如 `https://api.example.com/v1`） | 无 | 是 |
| `REALCRAFT_AI_API_KEY` | AI 认证密钥（仅服务端使用，不会出现在日志与响应中） | 无 | 是 |
| `REALCRAFT_AI_MODEL` | AI 视觉模型名 | 无 | 是 |
| `REALCRAFT_DATA_DIR` | 数据根目录（下含 `images`/`models` 子目录） | `./data` | 否 |

| `REALCRAFT_MAX_IMAGE_MB` | 单图片大小上限（MB），非法值回退默认 | `10` | 否 |

> 三个必填项（`REALCRAFT_AI_API_URL` / `REALCRAFT_AI_API_KEY` / `REALCRAFT_AI_MODEL`）缺失或为空时，服务将拒绝启动并报错。

配置方式（任选其一）：

```bash
# 1. 临时设置（当前 shell）
export REALCRAFT_AI_API_URL="https://api.example.com/v1"
export REALCRAFT_AI_API_KEY="sk-xxx"
export REALCRAFT_AI_MODEL="your-vision-model"

# 2. 使用 .env 文件 + docker-compose（见下文 Docker 部署）
```

## 本地启动

```bash
# 后端
cd backend
go mod tidy
REALCRAFT_AI_API_URL="https://api.example.com/v1" \
REALCRAFT_AI_API_KEY="sk-xxx" \
REALCRAFT_AI_MODEL="your-vision-model" \
go run .

# 前端（开发模式，Vite dev server，/api 代理到后端）
cd frontend
npm install
npm run dev   # 默认 http://localhost:5173，代理目标见 .env.development
```

## Docker 部署

### 镜像说明

镜像基于 `alpine:3.19`，仅包含 `server` 二进制、前端 `dist` 静态产物与数据挂载点，**不含** Go/Node 构建工具链。因此镜像构建前需先在外部完成编译（本地手动编译，或由 GitHub Actions 的 `build-and-push.yml` 流水线自动完成）。

### 方式一：本地手动构建镜像

```bash
# 1. 构建前端产物（Node 20+）
cd frontend
npm ci
npm run build
cd ..
cp -r frontend/dist dist

# 2. 编译后端静态二进制（Go 1.22+）
cd backend
go mod tidy
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o ../server .
cd ..

# 3. 构建镜像
docker build -t realcraft-platform .
```

### 方式二：使用 CI 推送的镜像

推送到 `main` 分支或手动触发 `build-and-push` 工作流后，镜像会推送至 GHCR：

```bash
docker pull ghcr.io/<owner>/<repo>:latest
```

### 运行容器

```bash
docker run -d \
  --name realcraft \
  -p 8080:8080 \
  -e REALCRAFT_AI_API_URL="https://api.example.com/v1" \
  -e REALCRAFT_AI_API_KEY="sk-xxx" \
  -e REALCRAFT_AI_MODEL="your-vision-model" \
  -e REALCRAFT_SERVER_PORT=8080 \
  -e REALCRAFT_MAX_IMAGE_MB=10 \
  -v realcraft-data:/app/data \
  --restart unless-stopped \
  realcraft-platform
```

参数说明：

| 参数 | 说明 |
|------|------|
| `-p 8080:8080` | 将宿主机 8080 端口映射到容器监听端口 |
| `-e REALCRAFT_*` | 注入环境变量（必填项必须提供，否则容器启动失败） |
| `-v realcraft-data:/app/data` | 挂载命名卷持久化图片与模型数据 |
| `--restart unless-stopped` | 容器异常退出或宿主机重启后自动恢复 |

### docker-compose 示例

```yaml
services:
  realcraft:
    image: ghcr.io/<owner>/<repo>:latest
    container_name: realcraft
    ports:
      - "8080:8080"
    environment:
      REALCRAFT_AI_API_URL: "https://api.example.com/v1"
      REALCRAFT_AI_API_KEY: "sk-xxx"
      REALCRAFT_AI_MODEL: "your-vision-model"
      REALCRAFT_SERVER_PORT: "8080"
      REALCRAFT_MAX_IMAGE_MB: "10"
    volumes:
      - realcraft-data:/app/data
    restart: unless-stopped

volumes:
  realcraft-data:
```

启动与停止：

```bash
docker compose up -d     # 启动
docker compose logs -f   # 查看日志
docker compose down      # 停止并移除容器（保留数据卷）
```

### 数据持久化

服务将用户上传的原图与生成的模型 JSON 持久化在容器的 `/app/data` 目录下：

- `images/`：原图，命名 `{uuid}_{index}.jpg`
- `models/`：模型文件，命名 `{uuid}.json`

请务必通过 `-v`（或 compose 的 `volumes`）将该目录挂载到宿主机或命名卷，否则容器重建后数据会丢失。

## 接口说明

### 上传转换

`POST /api/generate`，`Content-Type: multipart/form-data`，字段 `images`（1 张拼合三视图，或 3 张分立正/侧/俯视图）。

```json
// 成功响应
{"code":200,"message":"success","data":{"json_url":"http://<server-host>/models/3f2a8b9c.json"}}
// 失败响应（data 省略）
{"code":400,"message":"输入形态不合法：请上传 1 张拼合三视图或 3 张分立三视图"}
```

### 模型分发

`GET /models/:filename`，返回纯 JSON 紧凑二维数组：

```json
[["minecraft:stone",0,0,0],["minecraft:cobblestone",1,0,0],["minecraft:glass",0,1,0]]
```

## 错误码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 输入非法 |
| 413 | 文件过大或数量超限 |
| 422 | 数据解析失败 |
| 502 | 上游推理失败 |
| 500 | 存储失败 |

## 测试

单元测试按模块分批执行（每批不超过 64 个测试项）：

```bash
cd backend
go test ./config/... ./httpx/... ./llm/... ./api/... ./modelstore/... ./staticfs/...
```
