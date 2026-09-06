package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"

	"github.com/realcraft/backend/api"
	"github.com/realcraft/backend/config"
	"github.com/realcraft/backend/httpx"
	"github.com/realcraft/backend/llm"
	"github.com/realcraft/backend/modelstore"
	"github.com/realcraft/backend/staticfs"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("加载配置失败: %v", err)
	}

	if err := ensureDirs(cfg); err != nil {
		log.Fatalf("创建数据目录失败: %v", err)
	}

	llmClient := llm.NewClient(cfg.AIAPIURL, cfg.AIAPIKey, cfg.AIModel)
	converter := api.NewLLMConverter(llmClient)

	router := gin.New()
	router.Use(gin.Recovery())
	router.Use(requestLogger())

	router.POST("/api/generate", api.NewGenerateHandler(cfg, converter).Handle)
	router.GET("/models/:filename", modelstore.NewModelHandler(cfg).Serve)
	router.NoRoute(staticfs.NewStaticHandler(config.DefaultDistDir).Serve)

	srv := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.ServerPort),
		Handler: router,
	}

	go func() {
		httpx.Logger().Info("服务启动", "port", cfg.ServerPort)
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Fatalf("服务启动失败: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		log.Printf("优雅关闭失败: %v", err)
	}
	httpx.Logger().Info("服务已退出")
}

func ensureDirs(cfg *config.Config) error {
	for _, d := range []string{
		filepath.Join(cfg.DataDir, "images"),
		filepath.Join(cfg.DataDir, "models"),
	} {
		if err := os.MkdirAll(d, 0o755); err != nil {
			return err
		}
	}
	return nil
}

func requestLogger() gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		c.Next()
		httpx.Logger().Info("请求处理完成",
			"method", c.Request.Method,
			"path", c.Request.URL.Path,
			"status", c.Writer.Status(),
			"duration", time.Since(start).String(),
		)
	}
}