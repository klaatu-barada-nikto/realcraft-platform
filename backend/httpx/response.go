package httpx

// Response 统一响应结构，JSON 字段为 code/message/data，data 仅在非空时输出。
type Response struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}

// OK 构造成功响应。
func OK(data interface{}) *Response {
	return &Response{Code: CodeSuccess, Message: "success", Data: data}
}

// Fail 构造失败响应，message 为空时回退为错误码默认文案。
func Fail(code int, message string) *Response {
	if message == "" {
		message = Message(code)
	}
	return &Response{Code: code, Message: message}
}