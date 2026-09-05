package llm

import (
	"errors"
	"reflect"
	"testing"
)

func TestSanitizePlain(t *testing.T) {
	raw := `[["minecraft:stone",0,0,0],["minecraft:glass",1,0,1]]`
	got, err := Sanitize(raw)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	want := []VoxelBlock{
		{ID: "minecraft:stone", X: 0, Y: 0, Z: 0},
		{ID: "minecraft:glass", X: 1, Y: 0, Z: 1},
	}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("got %+v, want %+v", got, want)
	}
}

func TestSanitizeWithCodeFence(t *testing.T) {
	raw := "好的，结果如下：\n```json\n[[\"minecraft:stone\",0,0,0]]\n```\n希望有帮助"
	got, err := Sanitize(raw)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 1 || got[0].ID != "minecraft:stone" {
		t.Errorf("got %+v", got)
	}
}

func TestSanitizeWithSurroundingText(t *testing.T) {
	raw := "这是转换结果 [ [\"minecraft:cobblestone\", 1, 0, 0] ] 完成"
	got, err := Sanitize(raw)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 1 || got[0].ID != "minecraft:cobblestone" {
		t.Errorf("got %+v", got)
	}
}

func TestSanitizeTruncated(t *testing.T) {
	raw := `[["minecraft:stone",0,0,0],["minecraft:glass",1,0`
	got, err := Sanitize(raw)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 1 || got[0].ID != "minecraft:stone" {
		t.Errorf("got %+v, want only first complete block", got)
	}
}

func TestSanitizeTruncatedInner(t *testing.T) {
	raw := `[["minecraft:stone",0,0,0],["minecraft:glass",1,0,1],["minecraft:wood",2`
	got, err := Sanitize(raw)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 2 {
		t.Fatalf("got %d blocks, want 2", len(got))
	}
	if got[1].ID != "minecraft:glass" {
		t.Errorf("got %+v", got)
	}
}

func TestSanitizeInvalidNoBracket(t *testing.T) {
	if _, err := Sanitize("这里没有 JSON 数组"); !errors.Is(err, ErrInvalid) {
		t.Errorf("expected ErrInvalid, got %v", err)
	}
}

func TestSanitizeInvalidStructure(t *testing.T) {
	raw := `["not", "an", "array", "of", "quads"]`
	if _, err := Sanitize(raw); !errors.Is(err, ErrInvalid) {
		t.Errorf("expected ErrInvalid, got %v", err)
	}
}

func TestSanitizeInvalidQuadLength(t *testing.T) {
	raw := `[["minecraft:stone",0,0]]`
	if _, err := Sanitize(raw); !errors.Is(err, ErrInvalid) {
		t.Errorf("expected ErrInvalid, got %v", err)
	}
}

func TestSanitizeEmptyArray(t *testing.T) {
	if _, err := Sanitize("[]"); !errors.Is(err, ErrInvalid) {
		t.Errorf("expected ErrInvalid, got %v", err)
	}
}

func TestSanitizeInvalidID(t *testing.T) {
	raw := `[["no-namespace",0,0,0]]`
	if _, err := Sanitize(raw); !errors.Is(err, ErrInvalid) {
		t.Errorf("expected ErrInvalid, got %v", err)
	}
}

func TestSanitizeNegativeCoord(t *testing.T) {
	raw := `[["minecraft:stone",-1,0,0]]`
	if _, err := Sanitize(raw); !errors.Is(err, ErrInvalid) {
		t.Errorf("expected ErrInvalid, got %v", err)
	}
}

func TestSanitizeStringCoords(t *testing.T) {
	raw := `[["minecraft:stone","0","0","0"]]`
	got, err := Sanitize(raw)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if got[0].X != 0 || got[0].Y != 0 || got[0].Z != 0 {
		t.Errorf("got %+v", got)
	}
}