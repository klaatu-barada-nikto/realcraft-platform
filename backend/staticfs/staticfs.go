package staticfs

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/gin-gonic/gin"

	"github.com/realcraft/backend/config"
)

// StaticHandler 前端静态资源与 SPA 回退处理器。
type StaticHandler struct {
	cfg *config.Config
}

// NewStaticHandler 构造静态资源处理器。
func NewStaticHandler(cfg *config.Config) *StaticHandler {
	return &StaticHandler{cfg: cfg}
}

// Serve 优先命中 dist 静态文件，未命中且非 /api、/models 前缀时回退 index.html。
func (h *StaticHandler) Serve(c *gin.Context) {
	path := c.Request.URL.Path

	if isAPIPrefix(path) || isModelsPrefix(path) {
		c.Status(http.StatusNotFound)
		return
	}

	rel := strings.TrimPrefix(path, "/")
	if rel == "" {
		rel = "index.html"
	}

	full := filepath.Join(h.cfg.DistDir, filepath.FromSlash(rel))
	if info, err := os.Stat(full); err == nil && !info.IsDir() {
		c.File(full)
		return
	}

	indexPath := filepath.Join(h.cfg.DistDir, "index.html")
	if _, err := os.Stat(indexPath); err != nil {
		c.Status(http.StatusNotFound)
		return
	}
	c.File(indexPath)
}

func isAPIPrefix(path string) bool {
	return path == "/api" || strings.HasPrefix(path, "/api/")
}

func isModelsPrefix(path string) bool {
	return path == "/models" || strings.HasPrefix(path, "/models/")
}