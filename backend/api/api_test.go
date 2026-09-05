package api

import (
	"bytes"
	"context"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"

	"github.com/gin-gonic/gin"

	"github.com/realcraft/backend/config"
)

type mockConverter struct {
	raw string
	err error
}

func (m *mockConverter) Generate(ctx context.Context, images [][]byte) (string, error) {
	return m.raw, m.err
}

type testFile struct {
	filename string
	data     []byte
}

func newMultipartRequest(t *testing.T, files []testFile) *http.Request {
	t.Helper()
	body := &bytes.Buffer{}
	w := multipart.NewWriter(body)
	for _, f := range files {
		part, err := w.CreateFormFile("images", f.filename)
		if err != nil {
			t.Fatalf("create form file: %v", err)
		}
		if _, err := part.Write(f.data); err != nil {
			t.Fatalf("write part: %v", err)
		}
	}
	if err := w.Close(); err != nil {
		t.Fatalf("close writer: %v", err)
	}
	req := httptest.NewRequest(http.MethodPost, "/api/generate", body)
	req.Header.Set("Content-Type", w.FormDataContentType())
	return req
}

func pngData(size int) []byte {
	data := make([]byte, size)
	copy(data, []byte{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})
	return data
}

func newCfg(dataDir string, maxBytes int64) *config.Config {
	return &config.Config{DataDir: dataDir, MaxImageBytes: maxBytes}
}

func perform(t *testing.T, cfg *config.Config, conv Converter, files []testFile) *httptest.ResponseRecorder {
	t.Helper()
	gin.SetMode(gin.TestMode)
	router := gin.New()
	router.POST("/api/generate", NewGenerateHandler(cfg, conv).Handle)
	req := newMultipartRequest(t, files)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)
	return w
}

type respBody struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    *struct {
		JSONURL string `json:"json_url"`
	} `json:"data"`
}

func decodeResp(t *testing.T, w *httptest.ResponseRecorder) respBody {
	t.Helper()
	var r respBody
	if err := json.Unmarshal(w.Body.Bytes(), &r); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	return r
}

func TestGenerateCountInvalid(t *testing.T) {
	cases := []struct {
		name  string
		files []testFile
	}{
		{"zero", []testFile{}},
		{"two", []testFile{{"a.png", pngData(20)}, {"b.png", pngData(20)}}},
		{"four", []testFile{
			{"a.png", pngData(20)}, {"b.png", pngData(20)},
			{"c.png", pngData(20)}, {"d.png", pngData(20)},
		}},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			w := perform(t, newCfg(t.TempDir(), 1<<20), &mockConverter{}, tc.files)
			r := decodeResp(t, w)
			if r.Code != 400 {
				t.Errorf("code = %d, want 400", r.Code)
			}
			if r.Data != nil {
				t.Errorf("data should be nil on failure")
			}
		})
	}
}

func TestGenerateNonImage(t *testing.T) {
	w := perform(t, newCfg(t.TempDir(), 1<<20), &mockConverter{}, []testFile{{"a.txt", []byte("hello world")}})
	r := decodeResp(t, w)
	if r.Code != 400 {
		t.Errorf("code = %d, want 400", r.Code)
	}
}

func TestGenerateTooLarge(t *testing.T) {
	w := perform(t, newCfg(t.TempDir(), 10), &mockConverter{}, []testFile{{"a.png", pngData(100)}})
	r := decodeResp(t, w)
	if r.Code != 413 {
		t.Errorf("code = %d, want 413", r.Code)
	}
}

func TestGenerateSaveFailure(t *testing.T) {
	// 不创建 images 子目录，写入原图时失败
	w := perform(t, newCfg(t.TempDir(), 1<<20), &mockConverter{raw: `[["minecraft:stone",0,0,0]]`}, []testFile{{"a.png", pngData(20)}})
	r := decodeResp(t, w)
	if r.Code != 500 {
		t.Errorf("code = %d, want 500", r.Code)
	}
}

func TestGenerateAIFailure(t *testing.T) {
	dir := t.TempDir()
	if err := os.MkdirAll(filepath.Join(dir, "images"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(filepath.Join(dir, "models"), 0o755); err != nil {
		t.Fatal(err)
	}
	conv := &mockConverter{err: context.DeadlineExceeded}
	w := perform(t, newCfg(dir, 1<<20), conv, []testFile{{"a.png", pngData(20)}})
	r := decodeResp(t, w)
	if r.Code != 502 {
		t.Errorf("code = %d, want 502", r.Code)
	}
}

func TestGenerateParseFailure(t *testing.T) {
	dir := t.TempDir()
	if err := os.MkdirAll(filepath.Join(dir, "images"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(filepath.Join(dir, "models"), 0o755); err != nil {
		t.Fatal(err)
	}
	conv := &mockConverter{raw: "这不是 JSON"}
	w := perform(t, newCfg(dir, 1<<20), conv, []testFile{{"a.png", pngData(20)}})
	r := decodeResp(t, w)
	if r.Code != 422 {
		t.Errorf("code = %d, want 422", r.Code)
	}
}

func TestGenerateSuccess(t *testing.T) {
	dir := t.TempDir()
	if err := os.MkdirAll(filepath.Join(dir, "images"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(filepath.Join(dir, "models"), 0o755); err != nil {
		t.Fatal(err)
	}
	conv := &mockConverter{raw: `[["minecraft:stone",0,0,0],["minecraft:glass",1,0,1]]`}
	w := perform(t, newCfg(dir, 1<<20), conv, []testFile{{"a.png", pngData(20)}})
	r := decodeResp(t, w)
	if r.Code != 200 {
		t.Fatalf("code = %d, want 200 (message=%s)", r.Code, r.Message)
	}
	if r.Data == nil || r.Data.JSONURL == "" {
		t.Fatalf("json_url should be present, got %+v", r.Data)
	}

	// 校验模型文件落盘且内容为紧凑二维数组
	entries, err := os.ReadDir(filepath.Join(dir, "models"))
	if err != nil {
		t.Fatalf("read models dir: %v", err)
	}
	if len(entries) != 1 {
		t.Fatalf("models dir should contain 1 file, got %d", len(entries))
	}
	content, err := os.ReadFile(filepath.Join(dir, "models", entries[0].Name()))
	if err != nil {
		t.Fatalf("read model: %v", err)
	}
	if string(content) != `[["minecraft:stone",0,0,0],["minecraft:glass",1,0,1]]` {
		t.Errorf("model content = %s", content)
	}
}