package llm

import (
	"encoding/json"
	"errors"
	"fmt"
	"strings"
)

// VoxelBlock 方块四元组，序列化为紧凑数组 [id, x, y, z]。
type VoxelBlock struct {
	ID string
	X  int
	Y  int
	Z  int
}

// MarshalJSON 将方块序列化为紧凑数组，满足 Fabric 下游严格解析契约。
func (v VoxelBlock) MarshalJSON() ([]byte, error) {
	return json.Marshal([]interface{}{v.ID, v.X, v.Y, v.Z})
}

// ModelData 二维数组的领域封装，序列化为顶层紧凑二维数组 [[id,x,y,z],...]。
type ModelData struct {
	Blocks []VoxelBlock
}

// MarshalJSON 输出顶层紧凑二维数组，无包装对象、无额外字段。
func (m ModelData) MarshalJSON() ([]byte, error) {
	if m.Blocks == nil {
		return []byte("[]"), nil
	}
	return json.Marshal(m.Blocks)
}

// Validate 校验模型数据：非空、坐标非负、id 合法。
func (m ModelData) Validate() error {
	if len(m.Blocks) == 0 {
		return errors.New("模型数据为空")
	}
	for _, b := range m.Blocks {
		if b.X < 0 || b.Y < 0 || b.Z < 0 {
			return fmt.Errorf("坐标必须为非负整数: %d,%d,%d", b.X, b.Y, b.Z)
		}
		if !validBlockID(b.ID) {
			return fmt.Errorf("非法方块 ID: %q", b.ID)
		}
	}
	return nil
}

// validBlockID 校验 id 是否符合 Minecraft 命名空间 ID 形态（namespace:path）。
func validBlockID(id string) bool {
	if id == "" {
		return false
	}
	parts := strings.SplitN(id, ":", 2)
	if len(parts) != 2 {
		return false
	}
	ns, path := parts[0], parts[1]
	if ns == "" || path == "" {
		return false
	}
	for _, c := range ns {
		if !validIDChar(c) {
			return false
		}
	}
	for _, c := range path {
		if !validIDChar(c) {
			return false
		}
	}
	return true
}

func validIDChar(c rune) bool {
	return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-' || c == '/'
}