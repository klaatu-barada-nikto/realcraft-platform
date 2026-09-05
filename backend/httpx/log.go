package httpx

import (
	"log/slog"
	"os"
)

var logger = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))

// Logger 返回默认结构化日志器。
func Logger() *slog.Logger {
	return logger
}

// WithTaskID 返回携带 task_id 字段的日志器，用于关联同一次转换任务的关键操作。
func WithTaskID(taskID string) *slog.Logger {
	return logger.With("task_id", taskID)
}

// MaskSecret 对敏感密钥脱敏，仅保留末尾 4 个字符，其余以星号代替。
func MaskSecret(secret string) string {
	if secret == "" {
		return ""
	}
	if len(secret) <= 4 {
		return "****"
	}
	return "****" + secret[len(secret)-4:]
}