package staticfs

import (
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"

	"github.com/gin-gonic/gin"

	"github.com/realcraft/backend/config"
)

func serveStatic(t *testing.T, cfg *config.Config, path string) *httptest.ResponseRecorder {
	t.Helper()
	gin.SetMode(gin.TestMode)
	router := gin.New()
	router.NoRoute(NewStaticHandler(cfg).Serve)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, path, nil)
	router.ServeHTTP(w, req)
	return w
}

func TestStaticServeHit(t *testing.T) {
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "app.js"), []byte("console.log(1)"), 0o644); err != nil {
		t.Fatal(err)
	}
	w := serveStatic(t, &config.Config{DistDir: dir}, "/app.js")
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", w.Code)
	}
	if w.Body.String() != "console.log(1)" {
		t.Errorf("body = %q", w.Body.String())
	}
}

func TestStaticServeFallbackIndex(t *testing.T) {
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "index.html"), []byte("<html>app</html>"), 0o644); err != nil {
		t.Fatal(err)
	}
	w := serveStatic(t, &config.Config{DistDir: dir}, "/some/spa/route")
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", w.Code)
	}
	if w.Body.String() != "<html>app</html>" {
		t.Errorf("body = %q", w.Body.String())
	}
}

func TestStaticServeNoFallbackForAPI(t *testing.T) {
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "index.html"), []byte("<html>app</html>"), 0o644); err != nil {
		t.Fatal(err)
	}
	for _, path := range []string{"/api/generate", "/api/foo"} {
		w := serveStatic(t, &config.Config{DistDir: dir}, path)
		if w.Code != http.StatusNotFound {
			t.Errorf("path %s: status = %d, want 404", path, w.Code)
		}
	}
}

func TestStaticServeNoFallbackForModels(t *testing.T) {
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "index.html"), []byte("<html>app</html>"), 0o644); err != nil {
		t.Fatal(err)
	}
	for _, path := range []string{"/models/abc.json", "/models/abc/def"} {
		w := serveStatic(t, &config.Config{DistDir: dir}, path)
		if w.Code != http.StatusNotFound {
			t.Errorf("path %s: status = %d, want 404", path, w.Code)
		}
	}
}