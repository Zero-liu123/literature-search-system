package com.example.literaturesearchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.literaturesearchsystem.dto.LoginDTO;
import com.example.literaturesearchsystem.dto.RegisterDTO;
import com.example.literaturesearchsystem.entity.User;
import com.example.literaturesearchsystem.mapper.UserMapper;
import com.example.literaturesearchsystem.service.UserService;
import com.example.literaturesearchsystem.vo.LoginVO;
import com.example.literaturesearchsystem.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername())
                .eq(User::getDeleted, 0)
                .eq(User::getStatus, 0);

        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        String encryptedPassword = DigestUtils.md5DigestAsHex(
                loginDTO.getPassword().getBytes(StandardCharsets.UTF_8)
        );

        if (!user.getPassword().equals(encryptedPassword)) {
            throw new RuntimeException("密码错误");
        }

        // 创建登录响应
        LoginVO loginVO = new LoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setEmail(user.getEmail());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRole(user.getRole());

        return loginVO;
    }

    @Override
    public LoginVO register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDTO.getUsername())
                .eq(User::getDeleted, 0);

        if (userMapper.selectOne(queryWrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(DigestUtils.md5DigestAsHex(
                registerDTO.getPassword().getBytes(StandardCharsets.UTF_8)
        ));
        user.setNickname(registerDTO.getNickname());
        user.setEmail(registerDTO.getEmail());
        user.setRole(1);  // 默认普通用户
        user.setDeleted(0);
        user.setStatus(0);  // 正常状态
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);

        // 创建LoginVO
        LoginVO loginVO = new LoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setEmail(user.getEmail());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRole(user.getRole());

        return loginVO;
    }

    @Override
    public LoginVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 0 || user.getDeleted() != 0) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        LoginVO loginVO = new LoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setEmail(user.getEmail());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRole(user.getRole());

        return loginVO;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String encryptedOldPassword = DigestUtils.md5DigestAsHex(
                oldPassword.getBytes(StandardCharsets.UTF_8)
        );

        if (!user.getPassword().equals(encryptedOldPassword)) {
            return false;
        }

        user.setPassword(DigestUtils.md5DigestAsHex(
                newPassword.getBytes(StandardCharsets.UTF_8)
        ));

        userMapper.updateById(user);
        return true;
    }

    @Override
    public UserVO updateProfile(Long userId, String nickname, String email, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setNickname(nickname);
        if (email != null) {
            user.setEmail(email);
        }
        if (avatarUrl != null) {
            user.setAvatar(avatarUrl);
        }

        userMapper.updateById(user);

        // 返回UserVO
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setEmail(user.getEmail());
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());

        return userVO;
    }

    @Override
    public UserVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 0 || user.getDeleted() != 0) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setEmail(user.getEmail());
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());

        return userVO;
    }
}
