package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
)

const (
	// EnvServerPort 监听端口。
	EnvServerPort = "REALCRAFT_SERVER_PORT"
	// EnvAIAPIURL AI 推理服务地址（OpenAI 兼容）。
	EnvAIAPIURL = "REALCRAFT_AI_API_URL"
	// EnvAIAPIKey AI 认证密钥。
	EnvAIAPIKey = "REALCRAFT_AI_API_KEY"
	// EnvAIModel AI 模型名。
	EnvAIModel = "REALCRAFT_AI_MODEL"
	// EnvDataDir 数据根目录。
	EnvDataDir = "REALCRAFT_DATA_DIR"
	// EnvMaxImageMB 单图片大小上限（MB）。
	EnvMaxImageMB = "REALCRAFT_MAX_IMAGE_MB"

	// DefaultServerPort 默认监听端口。
	DefaultServerPort = 8080
	// DefaultDataDir 默认数据根目录。
	DefaultDataDir = "./data"
	// DefaultDistDir 前端产物目录（固定值，由镜像构建时确定，不支持环境变量覆盖）。
	DefaultDistDir = "./dist"
	// DefaultMaxImageMB 默认单图片大小上限（MB）。
	DefaultMaxImageMB = 10
)

// Config 运行配置，仅服务端持有。
type Config struct {
	ServerPort    int
	AIAPIURL      string
	AIAPIKey      string
	AIModel       string
	DataDir       string
	MaxImageBytes int64
}

// Load 从环境变量读取并校验配置，缺失必填项或数值非法时返回错误。
func Load() (*Config, error) {
	cfg := &Config{
		ServerPort: DefaultServerPort,
		DataDir:    DefaultDataDir,
	}

	if v := strings.TrimSpace(os.Getenv(EnvServerPort)); v != "" {
		p, err := strconv.Atoi(v)
		if err != nil {
			return nil, fmt.Errorf("invalid %s: %q is not an integer", EnvServerPort, v)
		}
		cfg.ServerPort = p
	}

	cfg.AIAPIURL = strings.TrimSpace(os.Getenv(EnvAIAPIURL))
	cfg.AIAPIKey = strings.TrimSpace(os.Getenv(EnvAIAPIKey))
	cfg.AIModel = strings.TrimSpace(os.Getenv(EnvAIModel))

	if cfg.AIAPIURL == "" {
		return nil, fmt.Errorf("missing required env %s", EnvAIAPIURL)
	}
	if cfg.AIAPIKey == "" {
		return nil, fmt.Errorf("missing required env %s", EnvAIAPIKey)
	}
	if cfg.AIModel == "" {
		return nil, fmt.Errorf("missing required env %s", EnvAIModel)
	}

	mb := DefaultMaxImageMB
	if v := strings.TrimSpace(os.Getenv(EnvMaxImageMB)); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n > 0 {
			mb = n
		}
	}
	cfg.MaxImageBytes = int64(mb) * 1024 * 1024

	return cfg, nil
}
