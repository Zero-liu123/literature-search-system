package com.example.literaturesearchsystem.vo;

import lombok.Data;

@Data
public class LoginVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private Integer role;
    private String token;  // JWT token
}
