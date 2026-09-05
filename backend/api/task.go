package api

import (
	"crypto/rand"
	"fmt"
)

// Task 任务标识，持有 UUID，派生原图与模型文件路径。
type Task struct {
	ID string
}

// NewTask 生成携带 UUID 的任务标识。
func NewTask() (*Task, error) {
	id, err := newUUID()
	if err != nil {
		return nil, err
	}
	return &Task{ID: id}, nil
}

// ImagePath 派生原图文件名 {uuid}_{index}.jpg。
func (t *Task) ImagePath(index int) string {
	return fmt.Sprintf("%s_%d.jpg", t.ID, index)
}

// ModelPath 派生模型文件名 {uuid}.json。
func (t *Task) ModelPath() string {
	return fmt.Sprintf("%s.json", t.ID)
}

func newUUID() (string, error) {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:16]), nil
}