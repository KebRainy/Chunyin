package com.example.demo1.dto.request;

import com.example.demo1.common.enums.MessagePolicy;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMessagePolicyRequest {
    @NotNull(message = "私信接收策略不能为空")
    private MessagePolicy messagePolicy;
}

