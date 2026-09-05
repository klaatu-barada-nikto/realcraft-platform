# realcraft-platform

像素三视图转 Minecraft 方块的极简一体化服务（Go 后端）。

## 项目简介

用户上传已像素化的物体三视图（正视、侧视、俯视），服务调用多模态 AI 进行空间推理，通过三视图投影交集运算输出确定性的 Minecraft 体素坐标，并以紧凑二维数组 JSON 文件对外分发，供 Fabric 模组通过 `/buildmodel <url>` 导入构建。

## 技术栈

- 后端：Go 1.22+，Gin
- 存储：本地文件系统（`./data/images` 存原图、`./data/models` 存模型 JSON），无数据库
- 托管：单进程统一托管 API 路由、模型静态分发与前端 `dist` 静态资源（SPA 回退），无 Nginx

## 环境变量

| 变量 | 含义 | 默认值 | 必填 |
|------|------|--------|------|
| `REALCRAFT_SERVER_PORT` | 监听端口 | `8080` | 否 |
| `REALCRAFT_AI_API_URL` | AI 推理服务地址（OpenAI 兼容） | 无 | 是 |
| `REALCRAFT_AI_API_KEY` | AI 认证密钥 | 无 | 是 |
| `REALCRAFT_AI_MODEL` | AI 模型名 | 无 | 是 |
| `REALCRAFT_DATA_DIR` | 数据根目录（含 images/models） | `./data` | 否 |
| `REALCRAFT_DIST_DIR` | 前端产物目录 | `./dist` | 否 |
| `REALCRAFT_MAX_IMAGE_MB` | 单图片大小上限（MB） | `10` | 否 |

## 启动方式

```bash
cd backend
go mod tidy
REALCRAFT_AI_API_URL="https://api.example.com/v1" \
REALCRAFT_AI_API_KEY="sk-xxx" \
REALCRAFT_AI_MODEL="your-vision-model" \
go run .
```

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
