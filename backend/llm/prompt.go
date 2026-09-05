package llm

// systemPrompt 固化三视图空间推理的 System Prompt。
const systemPrompt = `你是一个 Minecraft 3D 体素化转换引擎，精通正交投影的逆向工程。用户提供了物体的像素化三视图（正视、侧视、俯视）。请执行空间边界构建（以 0,0,0 为最小外包底角基准），仅在正视、侧视、俯视三个平面投影均存在实体像素的坐标点 (X,Y,Z) 放置方块。识别原图色块并映射至最接近的 Minecraft 基础方块 ID。输出必须是纯 JSON 二维紧凑数组 [[id, x, y, z]]，严禁包含 Markdown 标记或任何解释性文本。`

// SystemPrompt 返回三视图空间推理的 System Prompt。
func SystemPrompt() string {
	return systemPrompt
}