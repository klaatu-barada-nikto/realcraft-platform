package httpx

// 业务错误码定义。
const (
	// CodeSuccess 成功。
	CodeSuccess = 200
	// CodeBadRequest 输入非法（图片数量非 1/3 张、文件类型非图片）。
	CodeBadRequest = 400
	// CodeTooLarge 文件过大或数量超限。
	CodeTooLarge = 413
	// CodeUnprocessable 数据解析失败（AI 返回经清洗后仍非法）。
	CodeUnprocessable = 422
	// CodeUpstreamError 上游推理失败（AI 服务不可达/超时/返回错误）。
	CodeUpstreamError = 502
	// CodeInternalError 存储失败。
	CodeInternalError = 500
)

var codeMessage = map[int]string{
	CodeSuccess:       "success",
	CodeBadRequest:    "输入不合法",
	CodeTooLarge:      "文件过大或数量超限",
	CodeUnprocessable: "数据解析失败",
	CodeUpstreamError: "上游推理失败",
	CodeInternalError: "存储失败",
}

// Message 返回错误码对应的默认文案。
func Message(code int) string {
	if m, ok := codeMessage[code]; ok {
		return m
	}
	return "未知错误"
}