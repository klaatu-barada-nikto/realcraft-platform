package config

import "testing"

func clearEnv(t *testing.T) {
	t.Helper()
	for _, k := range []string{EnvServerPort, EnvAIAPIURL, EnvAIAPIKey, EnvAIModel, EnvDataDir, EnvDistDir, EnvMaxImageMB} {
		t.Setenv(k, "")
	}
}

func setRequired(t *testing.T) {
	t.Helper()
	t.Setenv(EnvAIAPIURL, "https://api.example.com/v1")
	t.Setenv(EnvAIAPIKey, "sk-test")
	t.Setenv(EnvAIModel, "test-model")
}

func TestLoadDefaults(t *testing.T) {
	clearEnv(t)
	setRequired(t)

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.ServerPort != DefaultServerPort {
		t.Errorf("ServerPort = %d, want %d", cfg.ServerPort, DefaultServerPort)
	}
	if cfg.DataDir != DefaultDataDir {
		t.Errorf("DataDir = %q, want %q", cfg.DataDir, DefaultDataDir)
	}
	if cfg.DistDir != DefaultDistDir {
		t.Errorf("DistDir = %q, want %q", cfg.DistDir, DefaultDistDir)
	}
	if want := int64(DefaultMaxImageMB) * 1024 * 1024; cfg.MaxImageBytes != want {
		t.Errorf("MaxImageBytes = %d, want %d", cfg.MaxImageBytes, want)
	}
}

func TestLoadMissingRequired(t *testing.T) {
	cases := []struct {
		name string
		set  func(t *testing.T)
	}{
		{"missing URL", func(t *testing.T) {
			t.Setenv(EnvAIAPIKey, "sk-test")
			t.Setenv(EnvAIModel, "test-model")
		}},
		{"missing Key", func(t *testing.T) {
			t.Setenv(EnvAIAPIURL, "https://api.example.com/v1")
			t.Setenv(EnvAIModel, "test-model")
		}},
		{"missing Model", func(t *testing.T) {
			t.Setenv(EnvAIAPIURL, "https://api.example.com/v1")
			t.Setenv(EnvAIAPIKey, "sk-test")
		}},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			clearEnv(t)
			tc.set(t)
			if _, err := Load(); err == nil {
				t.Fatalf("expected error, got nil")
			}
		})
	}
}

func TestLoadMaxImageBytes(t *testing.T) {
	clearEnv(t)
	setRequired(t)
	t.Setenv(EnvMaxImageMB, "5")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if want := int64(5) * 1024 * 1024; cfg.MaxImageBytes != want {
		t.Errorf("MaxImageBytes = %d, want %d", cfg.MaxImageBytes, want)
	}
}

func TestLoadMaxImageBytesInvalidFallsBack(t *testing.T) {
	clearEnv(t)
	setRequired(t)
	t.Setenv(EnvMaxImageMB, "not-a-number")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if want := int64(DefaultMaxImageMB) * 1024 * 1024; cfg.MaxImageBytes != want {
		t.Errorf("MaxImageBytes = %d, want %d", cfg.MaxImageBytes, want)
	}
}

func TestLoadEnvInjection(t *testing.T) {
	clearEnv(t)
	t.Setenv(EnvServerPort, "9090")
	t.Setenv(EnvAIAPIURL, "https://other.example.com/v1")
	t.Setenv(EnvAIAPIKey, "sk-other")
	t.Setenv(EnvAIModel, "other-model")
	t.Setenv(EnvDataDir, "/tmp/rc-data")
	t.Setenv(EnvDistDir, "/tmp/rc-dist")
	t.Setenv(EnvMaxImageMB, "20")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.ServerPort != 9090 {
		t.Errorf("ServerPort = %d, want 9090", cfg.ServerPort)
	}
	if cfg.AIAPIURL != "https://other.example.com/v1" {
		t.Errorf("AIAPIURL = %q", cfg.AIAPIURL)
	}
	if cfg.AIAPIKey != "sk-other" {
		t.Errorf("AIAPIKey = %q", cfg.AIAPIKey)
	}
	if cfg.AIModel != "other-model" {
		t.Errorf("AIModel = %q", cfg.AIModel)
	}
	if cfg.DataDir != "/tmp/rc-data" {
		t.Errorf("DataDir = %q", cfg.DataDir)
	}
	if cfg.DistDir != "/tmp/rc-dist" {
		t.Errorf("DistDir = %q", cfg.DistDir)
	}
	if want := int64(20) * 1024 * 1024; cfg.MaxImageBytes != want {
		t.Errorf("MaxImageBytes = %d, want %d", cfg.MaxImageBytes, want)
	}
}