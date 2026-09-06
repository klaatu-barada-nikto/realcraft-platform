package com.realcraft.platform.llm;

/**
 * AI 推理调用抽象，便于测试注入 mock。
 */
public interface LlmClient {

    /**
     * 将图片字节交给 AI 推理，返回模型原始文本。
     *
     * @param images 图片字节数组（每张一元素）
     * @return AI 返回的原始文本
     */
    String generate(byte[][] images);
}