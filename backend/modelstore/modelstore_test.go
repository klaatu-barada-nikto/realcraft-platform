package modelstore

import (
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"

	"github.com/gin-gonic/gin"

	"github.com/realcraft/backend/config"
)

func serveModel(t *testing.T, cfg *config.Config, filename string) *httptest.ResponseRecorder {
	t.Helper()
	gin.SetMode(gin.TestMode)
	router := gin.New()
	router.GET("/models/:filename", NewModelHandler(cfg).Serve)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/models/"+filename, nil)
	router.ServeHTTP(w, req)
	return w
}

func TestModelServeHit(t *testing.T) {
	dir := t.TempDir()
	modelsDir := filepath.Join(dir, "models")
	if err := os.MkdirAll(modelsDir, 0o755); err != nil {
		t.Fatal(err)
	}
	content := `[["minecraft:stone",0,0,0]]`
	if err := os.WriteFile(filepath.Join(modelsDir, "abc.json"), []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}

	w := serveModel(t, &config.Config{DataDir: dir}, "abc.json")
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", w.Code)
	}
	if ct := w.Header().Get("Content-Type"); ct != "application/json" {
		t.Errorf("content-type = %q, want application/json", ct)
	}
	if w.Body.String() != content {
		t.Errorf("body = %q, want %q", w.Body.String(), content)
	}
}

func TestModelServeNotFound(t *testing.T) {
	dir := t.TempDir()
	if err := os.MkdirAll(filepath.Join(dir, "models"), 0o755); err != nil {
		t.Fatal(err)
	}
	w := serveModel(t, &config.Config{DataDir: dir}, "missing.json")
	if w.Code != http.StatusNotFound {
		t.Errorf("status = %d, want 404", w.Code)
	}
}

func TestModelServePathTraversal(t *testing.T) {
	dir := t.TempDir()
	if err := os.MkdirAll(filepath.Join(dir, "models"), 0o755); err != nil {
		t.Fatal(err)
	}
	w := serveModel(t, &config.Config{DataDir: dir}, "..")
	if w.Code != http.StatusForbidden {
		t.Errorf("status = %d, want 403", w.Code)
	}
}

func TestSafeFilename(t *testing.T) {
	cases := []struct {
		name string
		want bool
	}{
		{"abc.json", true},
		{"3f2a8b9c.json", true},
		{"", false},
		{"..", false},
		{"../secret", false},
		{`..\..\secret`, false},
		{`a\b`, false},
		{"a/b", false},
		{"/etc/passwd", false},
	}
	for _, tc := range cases {
		if got := safeFilename(tc.name); got != tc.want {
			t.Errorf("safeFilename(%q) = %v, want %v", tc.name, got, tc.want)
		}
	}
}