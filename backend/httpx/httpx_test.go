package httpx

import (
	"encoding/json"
	"strings"
	"testing"
)

func TestOKResponse(t *testing.T) {
	resp := OK(map[string]string{"json_url": "http://host/models/x.json"})

	body, err := json.Marshal(resp)
	if err != nil {
		t.Fatalf("marshal error: %v", err)
	}
	var m map[string]interface{}
	if err := json.Unmarshal(body, &m); err != nil {
		t.Fatalf("unmarshal error: %v", err)
	}
	if m["code"] != float64(200) {
		t.Errorf("code = %v, want 200", m["code"])
	}
	if m["message"] != "success" {
		t.Errorf("message = %v, want success", m["message"])
	}
	if _, ok := m["data"]; !ok {
		t.Errorf("data should be present on success")
	}
}

func TestFailResponseOmitsData(t *testing.T) {
	resp := Fail(CodeBadRequest, "输入形态不合法")

	body, err := json.Marshal(resp)
	if err != nil {
		t.Fatalf("marshal error: %v", err)
	}
	var m map[string]interface{}
	if err := json.Unmarshal(body, &m); err != nil {
		t.Fatalf("unmarshal error: %v", err)
	}
	if m["code"] != float64(400) {
		t.Errorf("code = %v, want 400", m["code"])
	}
	if m["message"] != "输入形态不合法" {
		t.Errorf("message = %v", m["message"])
	}
	if _, ok := m["data"]; ok {
		t.Errorf("data should be omitted on failure")
	}
}

func TestFailDefaultMessage(t *testing.T) {
	resp := Fail(CodeUnprocessable, "")
	if resp.Message != "数据解析失败" {
		t.Errorf("message = %q, want default text", resp.Message)
	}
}

func TestMessage(t *testing.T) {
	cases := map[int]string{
		CodeSuccess:       "success",
		CodeBadRequest:    "输入不合法",
		CodeTooLarge:      "文件过大或数量超限",
		CodeUnprocessable: "数据解析失败",
		CodeUpstreamError: "上游推理失败",
		CodeInternalError: "存储失败",
	}
	for code, want := range cases {
		if got := Message(code); got != want {
			t.Errorf("Message(%d) = %q, want %q", code, got, want)
		}
	}
}

func TestMaskSecret(t *testing.T) {
	secret := "sk-1234567890abcdef"
	masked := MaskSecret(secret)
	if masked == "" {
		t.Fatal("masked should not be empty")
	}
	if strings.Contains(masked, secret) {
		t.Errorf("masked secret still contains plaintext: %q", masked)
	}
	if strings.Contains(masked, "1234567890") {
		t.Errorf("masked secret leaks key body: %q", masked)
	}
}

func TestMaskSecretShort(t *testing.T) {
	if got := MaskSecret("abc"); got != "****" {
		t.Errorf("MaskSecret(abc) = %q, want ****", got)
	}
	if got := MaskSecret(""); got != "" {
		t.Errorf("MaskSecret(\"\") = %q, want empty", got)
	}
}