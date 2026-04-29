package com.example.literaturesearchsystem.service;

import com.example.literaturesearchsystem.dto.LoginDTO;
import com.example.literaturesearchsystem.dto.RegisterDTO;
import com.example.literaturesearchsystem.vo.LoginVO;
import com.example.literaturesearchsystem.vo.UserVO;

public interface UserService {

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户注册
     */
    LoginVO register(RegisterDTO registerDTO);

    /**
     * 获取当前用户信息（简化版）
     */
    LoginVO getCurrentUser(Long userId);

    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 更新用户资料
     */
    UserVO updateProfile(Long userId, String nickname, String email, String avatarUrl);

    /**
     * 获取用户详细信息
     */
    UserVO getUserDetail(Long userId);
}