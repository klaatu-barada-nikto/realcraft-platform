package com.realcraft.platform.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 上传转换成功数据：模型访问地址，JSON 字段 json_url。
 */
public record GenerateData(@JsonProperty("json_url") String jsonUrl) {
}