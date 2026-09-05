# 开发实施计划 (plan.md) - Image to Voxel 极简一体化服务 (三视图版)

## 1. 项目定位与整体架构

本项目为“像素三视图转 Minecraft 方块”的轻量化 Demo 系统。为解决通用大模型空间幻觉问题，系统要求用户输入**已像素化的物体三视图（正视、侧视、俯视）**，利用通用多模态大模型的空间逻辑推理能力，通过三维矩阵交集运算输出确定性的体素坐标。

* **前端技术栈**: Vue 3 + Element Plus + Vite（开发环境依赖 **Node 20+**）。
* **后端技术栈**: Go (基于 Gin 或原生 `net/http`)，**兼具 API 路由与静态资源统一分发能力，完全剥离 Nginx**。
* **持久化方案**: 本地文件存储（`./data/images` 存原图，`./data/models` 存模型 JSON），无数据库。
* **构建与交付模式**:
* 由 **GitHub Actions** 承担完整编译（Node 20+ 编译前端产物 `dist`，Go 编译静态二进制可执行文件）。
* Docker 仅作为极简运行时环境（基于 Alpine），容器内仅包含 Go 二进制程序、前端 `dist` 目录与数据挂载点。



---

## 2. 路由与分发设计（Go 统一托管）

Go 服务端监听单一端口（如 `:8080`），负责处理三类流量：

```text
                    ┌───> /api/*        ───> 业务逻辑处理器 (接收三视图、调用AI、清洗截断JSON)
客户端请求 ──> Go服务 ────┼───> /models/*     ───> 本地模型静态文件分发 (供 Fabric 插件拉取)
                    └───> /* (所有其他) ───> 前端 dist 静态资源 (支持 SPA History 路由回退)

```

1. **API 路由**: `POST /api/generate` (接收上传图片并触发转换)
2. **模型资源路由**: `GET /models/:filename` (静态映射到 `./data/models/:filename`)
3. **前端资源与 SPA 回退**: 静态映射到 `./dist` 目录。当请求的文件不存在且非 `/api` 或 `/models` 前缀时，默认响应 `dist/index.html`。

---

## 3. 接口与数据协议规范

### 3.1 模型 JSON 数据格式

生成的模型文件必须严格遵循紧凑型二维数组格式，元素顺序为 `[id, x, y, z]`：

```json
[
  ["minecraft:stone", 0, 0, 0],
  ["minecraft:cobblestone", 1, 0, 0],
  ["minecraft:glass", 0, 1, 0]
]

```

* `0`: 方块完整命名空间 ID（如 `minecraft:stone`）。
* `1 / 2 / 3`: 相对原点的空间偏移坐标（以 `0, 0, 0` 为最小外包底角）。

### 3.2 文件上传接口

* **Path**: `POST /api/generate`
* **Content-Type**: `multipart/form-data`
* **Body**: `images` (支持 1 张拼合的三视图，或 3 张分立的正/侧/俯视图图片)
* **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "json_url": "http://<server-host>/models/3f2a8b9c.json"
  }
}

```



---

## 4. 任务拆解与实施步骤 (Task Breakdown)

### Task 1: 前端上传界面与预期管理 (Vue 3 + Node 20+)

* **初始化工程**: 使用 Vite 创建 Vue 3 模板，锁定 Node 20+ 环境。
* **UI 布局与引导**:
* 界面顶部增加**核心引导区**，明确要求：“请上传利用 AI 或工具生成的**像素化三视图**（正视图、侧视图、俯视图）”。
* 提供一个标准的像素三视图示例图片（UI 上并排展示正、侧、俯视小图），建立正确的用户预期。


* **上传交互**:
* 使用 Element Plus `<el-upload>`，配置 `list-type="picture-card"`，支持多文件选择（最多3张），关闭自动上传。
* 点击“生成方块模型”时，将文件封装至 `FormData` 提交至 `/api/generate`，并展示全局 Loading。


* **结果呈现**:
* 成功后展示 `json_url`，并提供一键复制功能（如复制指令：`/buildmodel <url>` 对应 Fabric 插件的指令）。



### Task 2: Go 一体化服务器基础搭建

* **目录初始化与挂载**:
* 程序启动时检查并创建目录：`./data/images`, `./data/models`。


* **静态路由分发**:
* 配置 `/models/*` 路由组拦截器，直接返回 `./data/models` 目录下的 JSON 文件。
* 配置 `/*` 路由，优先在 `./dist` 查找静态文件（js, css, img），若 `os.IsNotExist`，则统一返回 `./dist/index.html`（解决 Vue Router History 模式 404 问题）。


* **接收上传任务**:
* 解析 `/api/generate` 的 `multipart/form-data`。
* 为任务生成 UUID，将图片按 `{uuid}_{index}.jpg` 写入磁盘，准备将其转换为 Base64 供下一步调用。



### Task 3: 严格的三视图 Prompt 构建与 JSON 容错清洗 (核心算法层)

* **多模态 API 客户端**:
* 封装 HTTP 客户端请求兼容 OpenAI 格式的 Vision API。将步骤2的图片转为 Base64 并入 `messages`。


* **Prompt 工程设定**:
* *System Prompt*: “你是一个 Minecraft 3D 体素化转换引擎，精通正交投影的逆向工程。用户提供了物体的像素化三视图。请执行空间边界构建（以 0,0,0 为基准），仅在正视、侧视、俯视三个平面投影均存在实体像素的坐标点 (X,Y,Z) 放置方块。识别原图色块并映射至最接近的 Minecraft 基础方块 ID。输出必须是纯 JSON 二维紧凑数组 `[[id, x, y, z]]`，严禁包含 Markdown 标记或任何解释。”


* **JSON 容错与清洗逻辑**:
* 大模型返回结果后，先用正则替换掉可能出现的 `json` 和 `。
* **截断修复算法**：由于输出可能因 Token 限制被截断，后端需从字符串末尾反向查找最后一个完整的 `]`。如果数组未正常闭合，丢弃最后一个不完整的子数组，并强制追加 `]` 使其成为合法的 JSON。
* 校验通过后，将最终字符串写入 `./data/models/{uuid}.json`。



### Task 4: GitHub Actions 自动化构建与轻量化 Docker 封装

* **极简 Dockerfile**:
```dockerfile
FROM alpine:3.19
WORKDIR /app
RUN apk add --no-cache ca-certificates tzdata
# 依赖外部传入编译好的产物
COPY server /app/server
COPY dist /app/dist
RUN mkdir -p /app/data/images /app/data/models
EXPOSE 8080
VOLUME ["/app/data"]
ENTRYPOINT ["/app/server"]

```


* **GitHub Actions 流水线 (`.github/workflows/deploy.yml`)**:
1. **Frontend Build**: 使用 `actions/setup-node@v4` (node-version: 20)。执行 `npm ci && npm run build`。
2. **Backend Build**: 使用 `actions/setup-go@v5` (go-version: 1.22+)。执行 `CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o server ./backend`。
3. **Docker Build & Push**: 将前端生成的 `dist` 目录和后端生成的 `server` 二进制文件放置于同一层级，调用 `docker/build-push-action` 进行镜像打包并推送到目标镜像仓库（如 GHCR 或 Docker Hub）。



---

## 5. 项目输出目录结构约定

```text
.
├── .github/
│   └── workflows/
│       └── build-and-push.yml   # CI/CD 流水线，负责 Node 20+ 和 Go 的编译及镜像推送
├── backend/
│   ├── main.go                  # 核心路由分发、SPA 兼容逻辑
│   ├── api/                     # 上传处理逻辑
│   ├── llm/                     # 封装 Vision API 请求、Prompt 组装、JSON 截断清洗容错
│   └── go.mod
├── frontend/
│   ├── src/                     # Vue 3 页面 (包含三视图 UI 示例引导)
│   ├── package.json             # 依赖 (需在 Node 20+ 环境运行)
│   └── vite.config.js
├── Dockerfile                   # 最终运行时镜像 (仅含 Alpine, server 二进制, dist 静态包)
└── README.md

```