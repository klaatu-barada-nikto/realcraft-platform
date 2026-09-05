package modelstore

import (
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/gin-gonic/gin"

	"github.com/realcraft/backend/config"
)

// ModelHandler 模型文件分发处理器。
type ModelHandler struct {
	cfg *config.Config
}

// NewModelHandler 构造模型分发处理器。
func NewModelHandler(cfg *config.Config) *ModelHandler {
	return &ModelHandler{cfg: cfg}
}

// Serve 处理 GET /models/:filename。
func (h *ModelHandler) Serve(c *gin.Context) {
	filename := c.Param("filename")
	if !safeFilename(filename) {
		c.Status(http.StatusForbidden)
		return
	}

	data, err := os.ReadFile(filepath.Join(h.cfg.DataDir, "models", filename))
	if err != nil {
		if os.IsNotExist(err) {
			c.Status(http.StatusNotFound)
			return
		}
		c.Status(http.StatusInternalServerError)
		return
	}
	c.Data(http.StatusOK, "application/json", data)
}

// safeFilename 校验文件名，拒绝目录穿越、反斜杠与绝对路径。
func safeFilename(name string) bool {
	if name == "" {
		return false
	}
	if strings.Contains(name, "..") {
		return false
	}
	if strings.ContainsAny(name, `\/`) {
		return false
	}
	if filepath.IsAbs(name) {
		return false
	}
	return filepath.Base(name) == name
}