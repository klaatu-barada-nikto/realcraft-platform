package llm

import (
	"encoding/json"
	"errors"
	"strconv"
	"strings"
)

// ErrInvalid 表示 AI 返回数据经清洗后仍不合法。
var ErrInvalid = errors.New("数据解析失败")

// Sanitize 对 AI 原始返回文本执行清洗管道：噪声剥离 → 截断修复 → 结构校验 → 规范化。
func Sanitize(raw string) ([]VoxelBlock, error) {
	candidate := extractCandidate(raw)
	if candidate == "" {
		return nil, ErrInvalid
	}

	parsed, ok := parseJSON(candidate)
	if !ok {
		repaired := repairTruncation(candidate)
		parsed, ok = parseJSON(repaired)
		if !ok {
			return nil, ErrInvalid
		}
	}

	return normalize(parsed)
}

// extractCandidate 定位第一个 '[' 与最后一个 ']' 之间的候选 JSON 子串。
func extractCandidate(raw string) string {
	start := strings.Index(raw, "[")
	end := strings.LastIndex(raw, "]")
	if start == -1 || end == -1 || end <= start {
		return ""
	}
	return raw[start : end+1]
}

// repairTruncation 从末尾反向扫描最后一个已闭合的子数组，丢弃不完整尾部并补 ']' 闭合。
func repairTruncation(s string) string {
	idx := strings.LastIndex(s, "]")
	for idx >= 0 {
		sub := s[:idx+1] + "]"
		if json.Valid([]byte(sub)) {
			return sub
		}
		idx = strings.LastIndex(s[:idx], "]")
	}
	return ""
}

func parseJSON(s string) ([]interface{}, bool) {
	var out []interface{}
	if err := json.Unmarshal([]byte(s), &out); err != nil {
		return nil, false
	}
	return out, true
}

// normalize 校验结构（二维数组 + 四元组 [id,x,y,z]）并构造方块数据。
func normalize(parsed []interface{}) ([]VoxelBlock, error) {
	if len(parsed) == 0 {
		return nil, ErrInvalid
	}
	blocks := make([]VoxelBlock, 0, len(parsed))
	for _, elem := range parsed {
		arr, ok := elem.([]interface{})
		if !ok || len(arr) != 4 {
			return nil, ErrInvalid
		}
		id, ok := arr[0].(string)
		if !ok || !validBlockID(id) {
			return nil, ErrInvalid
		}
		x, ok1 := toInt(arr[1])
		y, ok2 := toInt(arr[2])
		z, ok3 := toInt(arr[3])
		if !ok1 || !ok2 || !ok3 {
			return nil, ErrInvalid
		}
		if x < 0 || y < 0 || z < 0 {
			return nil, ErrInvalid
		}
		blocks = append(blocks, VoxelBlock{ID: id, X: x, Y: y, Z: z})
	}
	return blocks, nil
}

func toInt(v interface{}) (int, bool) {
	switch n := v.(type) {
	case float64:
		if n != float64(int(n)) {
			return 0, false
		}
		return int(n), true
	case int:
		return n, true
	case string:
		i, err := strconv.Atoi(strings.TrimSpace(n))
		if err != nil {
			return 0, false
		}
		return i, true
	default:
		return 0, false
	}
}