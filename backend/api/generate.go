package api

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"

	"github.com/gin-gonic/gin"

	"github.com/realcraft/backend/config"
	"github.com/realcraft/backend/httpx"
	"github.com/realcraft/backend/llm"
)

// Converter 抽象 AI 推理调用，便于测试注入 mock。
type Converter interface {
	Generate(ctx context.Context, images [][]byte) (string, error)
}

// LLMConverter 基于 llm.Client 的转换器实现。
type LLMConverter struct {
	client *llm.Client
}

// NewLLMConverter 构造默认转换器。
func NewLLMConverter(client *llm.Client) *LLMConverter {
	return &LLMConverter{client: client}
}

// Generate 调用底层 AI 推理客户端。
func (c *LLMConverter) Generate(ctx context.Context, images [][]byte) (string, error) {
	return c.client.Generate(ctx, images)
}

// GenerateHandler 上传转换处理器。
type GenerateHandler struct {
	cfg       *config.Config
	converter Converter
}

// NewGenerateHandler 构造上传转换处理器。
func NewGenerateHandler(cfg *config.Config, converter Converter) *GenerateHandler {
	return &GenerateHandler{cfg: cfg, converter: converter}
}

// Handle 处理 POST /api/generate。
func (h *GenerateHandler) Handle(c *gin.Context) {
	task, err := NewTask()
	if err != nil {
		httpx.Logger().Error("生成任务标识失败", "error", err)
		c.JSON(http.StatusOK, httpx.Fail(httpx.CodeInternalError, "存储失败"))
		return
	}
	log := httpx.WithTaskID(task.ID)

	form, err := c.MultipartForm()
	if err != nil {
		log.Error("解析 multipart 失败", "error", err)
		c.JSON(http.StatusOK, httpx.Fail(httpx.CodeBadRequest, "解析上传数据失败"))
		return
	}
	files := form.File["images"]

	if len(files) != 1 && len(files) != 3 {
		c.JSON(http.StatusOK, httpx.Fail(httpx.CodeBadRequest, "输入形态不合法：请上传 1 张拼合三视图或 3 张分立三视图"))
		return
	}

	images := make([][]byte, 0, len(files))
	for _, fh := range files {
		if !isImage(fh) {
			c.JSON(http.StatusOK, httpx.Fail(httpx.CodeBadRequest, "文件类型不合法：仅支持图片"))
			return
		}
		if fh.Size > h.cfg.MaxImageBytes {
			c.JSON(http.StatusOK, httpx.Fail(httpx.CodeTooLarge, "文件过大或数量超限"))
			return
		}
		data, err := readFile(fh)
		if err != nil {
			log.Error("读取上传文件失败", "error", err)
			c.JSON(http.StatusOK, httpx.Fail(httpx.CodeBadRequest, "读取上传文件失败"))
			return
		}
		images = append(images, data)
	}

	imageDir := filepath.Join(h.cfg.DataDir, "images")
	for i, data := range images {
		if err := os.WriteFile(filepath.Join(imageDir, task.ImagePath(i)), data, 0o644); err != nil {
			log.Error("保存原图失败", "error", err)
			c.JSON(http.StatusOK, httpx.Fail(httpx.CodeInternalError, "存储失败"))
			return
		}
	}
	log.Info("原图已保存", "count", len(images))

	raw, err := h.converter.Generate(c.Request.Context(), images)
	if err != nil {
		log.Error("AI 推理失败", "error", err)
		c.JSON(http.StatusOK, httpx.Fail(httpx.CodeUpstreamError, "上游推理失败"))
		return
	}

	blocks, err := llm.Sanitize(raw)
	if err != nil {
		log.Error("数据清洗失败", "error", err)
		c.JSON(http.StatusOK, httpx.Fail(httpx.CodeUnprocessable, "数据解析失败"))
		return
	}

	model := llm.ModelData{Blocks: blocks}
	if err := model.Validate(); err != nil {
		log.Error("模型数据校验失败", "error", err)
		c.JSON(http.StatusOK, httpx.Fail(httpx.CodeUnprocessable, "数据解析失败"))
		return
	}

	payload, err := json.Marshal(model)
	if err != nil {
		log.Error("序列化模型失败", "error", err)
		c.JSON(http.StatusOK, httpx.Fail(httpx.CodeInternalError, "存储失败"))
		return
	}
	modelPath := filepath.Join(h.cfg.DataDir, "models", task.ModelPath())
	if err := os.WriteFile(modelPath, payload, 0o644); err != nil {
		log.Error("写入模型失败", "error", err)
		c.JSON(http.StatusOK, httpx.Fail(httpx.CodeInternalError, "存储失败"))
		return
	}
	log.Info("模型已生成", "model", task.ModelPath())

	jsonURL := fmt.Sprintf("http://%s/models/%s", c.Request.Host, task.ModelPath())
	c.JSON(http.StatusOK, httpx.OK(map[string]string{"json_url": jsonURL}))
}

func isImage(fh *multipart.FileHeader) bool {
	f, err := fh.Open()
	if err != nil {
		return false
	}
	defer f.Close()

	head := make([]byte, 512)
	n, _ := f.Read(head)
	ct := http.DetectContentType(head[:n])
	switch ct {
	case "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp":
		return true
	default:
		return false
	}
}

func readFile(fh *multipart.FileHeader) ([]byte, error) {
	f, err := fh.Open()
	if err != nil {
		return nil, err
	}
	defer f.Close()
	return io.ReadAll(f)
}