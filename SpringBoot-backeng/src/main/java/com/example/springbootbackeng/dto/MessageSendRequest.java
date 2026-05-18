package com.example.springbootbackeng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageSendRequest {
    @NotBlank(message = "收件人不能为空")
    @Size(max = 64, message = "收件人长度不能超过64")
    private String receiver; // 可传 userId 或 username

    @Size(max = 128, message = "主题长度不能超过128")
    private String subject;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容长度不能超过2000")
    private String content;
}
