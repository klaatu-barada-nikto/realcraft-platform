package llm

import (
	"encoding/json"
	"testing"
)

func TestVoxelBlockMarshalJSON(t *testing.T) {
	b := VoxelBlock{ID: "minecraft:stone", X: 0, Y: 1, Z: 2}
	got, err := json.Marshal(b)
	if err != nil {
		t.Fatalf("marshal error: %v", err)
	}
	if want := `["minecraft:stone",0,1,2]`; string(got) != want {
		t.Errorf("got %s, want %s", got, want)
	}
}

func TestModelDataMarshalJSON(t *testing.T) {
	m := ModelData{Blocks: []VoxelBlock{
		{ID: "minecraft:stone", X: 0, Y: 0, Z: 0},
		{ID: "minecraft:glass", X: 1, Y: 0, Z: 1},
	}}
	got, err := json.Marshal(m)
	if err != nil {
		t.Fatalf("marshal error: %v", err)
	}
	want := `[["minecraft:stone",0,0,0],["minecraft:glass",1,0,1]]`
	if string(got) != want {
		t.Errorf("got %s, want %s", got, want)
	}
}

func TestModelDataMarshalJSONEmpty(t *testing.T) {
	m := ModelData{}
	got, err := json.Marshal(m)
	if err != nil {
		t.Fatalf("marshal error: %v", err)
	}
	if string(got) != "[]" {
		t.Errorf("got %s, want []", got)
	}
}

func TestModelDataValidate(t *testing.T) {
	t.Run("empty", func(t *testing.T) {
		m := ModelData{}
		if err := m.Validate(); err == nil {
			t.Fatal("expected error for empty blocks")
		}
	})
	t.Run("negative coord", func(t *testing.T) {
		m := ModelData{Blocks: []VoxelBlock{{ID: "minecraft:stone", X: -1, Y: 0, Z: 0}}}
		if err := m.Validate(); err == nil {
			t.Fatal("expected error for negative coordinate")
		}
	})
	t.Run("invalid id", func(t *testing.T) {
		m := ModelData{Blocks: []VoxelBlock{{ID: "no-namespace", X: 0, Y: 0, Z: 0}}}
		if err := m.Validate(); err == nil {
			t.Fatal("expected error for invalid block id")
		}
	})
	t.Run("valid", func(t *testing.T) {
		m := ModelData{Blocks: []VoxelBlock{{ID: "minecraft:stone", X: 0, Y: 0, Z: 0}}}
		if err := m.Validate(); err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
	})
}

func TestValidBlockID(t *testing.T) {
	cases := []struct {
		id   string
		want bool
	}{
		{"minecraft:stone", true},
		{"minecraft:red_wool", true},
		{"mod:block/path", true},
		{"minecraft:cobblestone", true},
		{"", false},
		{"stone", false},
		{"minecraft:", false},
		{":stone", false},
		{"Minecraft:Stone", false},
		{"minecraft:stone stone", false},
	}
	for _, tc := range cases {
		if got := validBlockID(tc.id); got != tc.want {
			t.Errorf("validBlockID(%q) = %v, want %v", tc.id, got, tc.want)
		}
	}
}