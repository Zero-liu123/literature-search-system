package com.example.literaturesearchsystem.controller;

import com.example.literaturesearchsystem.common.Result;
import com.example.literaturesearchsystem.dto.LoginDTO;
import com.example.literaturesearchsystem.dto.RegisterDTO;
import com.example.literaturesearchsystem.entity.User;
import com.example.literaturesearchsystem.mapper.UserMapper;
import com.example.literaturesearchsystem.service.UserService;
import com.example.literaturesearchsystem.util.FileUploadUtil;
import com.example.literaturesearchsystem.util.JwtUtil;
import com.example.literaturesearchsystem.vo.LoginVO;
import com.example.literaturesearchsystem.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final FileUploadUtil fileUploadUtil;

    /**
     * MD5 加密密码（与 login/register 保持一致，不加盐）
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody RegisterDTO registerDTO) {
        try {
            LoginVO result = userService.register(registerDTO);
            // 生成 JWT token
            String token = jwtUtil.generateToken(result.getId(), result.getUsername(), result.getRole());
            result.setToken(token);
            return Result.successWithMsg("注册成功", result);
        } catch (Exception e) {
            log.error("注册失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            LoginVO result = userService.login(loginDTO);
            // 生成 JWT token
            String token = jwtUtil.generateToken(result.getId(), result.getUsername(), result.getRole());
            result.setToken(token);
            return Result.successWithMsg("登录成功", result);
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息（简化版）
     */
    @GetMapping("/current")
    public Result<LoginVO> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return Result.error("未登录");
        }

        // 去掉 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtUtil.validateToken(token)) {
            return Result.error("登录已过期");
        }

        Long userId = jwtUtil.getUserId(token);
        LoginVO user = userService.getCurrentUser(userId);
        return Result.success(user);
    }

    /**
     * 获取所有用户列表（仅管理员）
     */
    @GetMapping("/list")
    public Result<List<User>> getUserList(@RequestHeader("Authorization") String token) {
        // 验证管理员权限
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Integer role = jwtUtil.getRole(token);
        if (role == null || role != 2) {
            return Result.error("权限不足");
        }

        List<User> users = userMapper.selectList(null);
        // 隐藏密码
        users.forEach(user -> user.setPassword(null));
        return Result.success(users);
    }

    /**
     * 更新用户角色（仅管理员）
     */
    @PutMapping("/{id}/role")
    public Result<Void> updateUserRole(
            @PathVariable Long id,
            @RequestParam Integer role,
            @RequestHeader("Authorization") String token) {
        // 验证管理员权限
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Integer adminRole = jwtUtil.getRole(token);
        if (adminRole == null || adminRole != 2) {
            return Result.error("权限不足");
        }

        // 获取当前登录用户的ID
        Long currentUserId = jwtUtil.getUserId(token);

        // 不能修改自己的角色（防止把自己降级后无法管理）
        if (currentUserId != null && currentUserId.equals(id)) {
            return Result.error("不能修改自己的角色");
        }

        // 获取目标用户当前信息
        User targetUser = userMapper.selectById(id);  // ✅ 修复：userMapper1改为userMapper
        if (targetUser == null) {
            return Result.error("用户不存在");
        }

        // ✅ 修复：使用整数比较
        int adminCount = userMapper.countByRole(2);

        // ✅ 修复：使用整数比较
        if (targetUser.getRole() == 2 && adminCount == 1 && role != 2) {
            return Result.error("至少需要保留一名管理员，无法修改角色");
        }

        User user = new User();
        user.setId(id);
        // ✅ 修复：使用Integer类型
        user.setRole(role);
        userMapper.updateById(user);
        return Result.success(null);
    }
    /**
     * 获取当前用户详细信息
     */
    @GetMapping("/detail")
    public Result<UserVO> getUserDetail(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // ✅ 使用service方法，而不是直接操作数据库
        UserVO vo = userService.getUserDetail(userId);
        return Result.success(vo);
    }

    /**
     * 更新用户资料
     */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(
            @RequestParam("nickname") String nickname,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "avatar", required = false) MultipartFile avatarFile,
            @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 处理头像上传
        String avatarUrl = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarUrl = fileUploadUtil.uploadFile(avatarFile);
        }

        // ✅ 使用service方法，而不是直接操作数据库
        UserVO vo = userService.updateProfile(userId, nickname, email, avatarUrl);
        return Result.success(vo);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestBody Map<String, String> params,
            @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        // ✅ 使用service方法，而不是直接操作数据库
        boolean success = userService.changePassword(userId, oldPassword, newPassword);
        if (success) {
            return Result.success(null);
        } else {
            return Result.error("当前密码错误");
        }
    }

    /**
     * 用户注销自己的账户（物理删除，用户名可重新注册）
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteOwnAccount(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 物理删除用户记录
        userMapper.deleteById(userId);

        return Result.success(null);
    }

    /**
     * 管理员禁用用户
     */
    @PutMapping("/{id}/disable")
    public Result<Void> disableUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Integer role = jwtUtil.getRole(token);
        if (role == null || role != 2) {
            return Result.error("权限不足");
        }

        // 获取当前登录用户的ID
        Long currentUserId = jwtUtil.getUserId(token);

        // 不能禁用自己
        if (currentUserId != null && currentUserId.equals(id)) {
            return Result.error("不能禁用自己");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // ✅ 修复：使用整数比较
        if (user.getRole() == 2) {
            int adminCount = userMapper.countByRole(2);
            if (adminCount == 1) {
                return Result.error("至少需要保留一名管理员，无法禁用");
            }
        }

        // ✅ 修复：使用Integer类型
        user.setStatus(1);  // 禁用状态设为 1
        userMapper.updateById(user);
        return Result.success(null);
    }

    /**
     * 管理员启用用户
     */
    @PutMapping("/{id}/enable")
    public Result<Void> enableUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Integer role = jwtUtil.getRole(token);
        if (role == null || role != 2) {
            return Result.error("权限不足");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // ✅ 修复：使用Integer类型
        user.setStatus(0);  // 启用状态设为 0
        userMapper.updateById(user);

        return Result.success(null);
    }

    /**
     * 管理员永久删除用户
     */
    @DeleteMapping("/{id}/permanent")
    public Result<Void> permanentDeleteUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Integer role = jwtUtil.getRole(token);
        if (role == null || role != 2) {
            return Result.error("权限不足");
        }

        // 获取当前登录用户的ID
        Long currentUserId = jwtUtil.getUserId(token);

        // 不能删除自己
        if (currentUserId != null && currentUserId.equals(id)) {
            return Result.error("不能删除自己的账号");
        }

        // 获取目标用户信息
        User targetUser = userMapper.selectById(id);
        if (targetUser == null) {
            return Result.error("用户不存在");
        }

        // ✅ 修复：使用整数比较
        if (targetUser.getRole() == 2) {
            int adminCount = userMapper.countByRole(2);
            if (adminCount == 1) {
                return Result.error("至少需要保留一名管理员，无法删除");
            }
        }

        userMapper.deleteById(id);
        return Result.success(null);
    }

    /**
     * 管理员重置用户密码（重置为 123456）
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        // 验证管理员权限
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Integer role = jwtUtil.getRole(token);
        if (role == null || role != 2) {
            return Result.error("权限不足");
        }

        // 获取目标用户
        User targetUser = userMapper.selectById(id);
        if (targetUser == null) {
            return Result.error("用户不存在");
        }

        // 不能重置自己的密码（防止管理员把自己锁了）
        Long currentUserId = jwtUtil.getUserId(token);
        if (currentUserId != null && currentUserId.equals(id)) {
            return Result.error("不能重置自己的密码");
        }

        // 重置密码为 123456（使用与注册相同的加密方式）
        String defaultPassword = encryptPassword("123456");
        targetUser.setPassword(defaultPassword);
        userMapper.updateById(targetUser);

        log.info("管理员重置用户密码成功，用户ID: {}, 用户名: {}", id, targetUser.getUsername());
        return Result.success(null);
    }

}
