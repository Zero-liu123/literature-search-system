package com.example.literaturesearchsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.literaturesearchsystem.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    // ========== 新增以下方法 ==========

    /**
     * 统计指定角色的用户数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE role = #{role} AND deleted = 0")
    int countByRole(@org.apache.ibatis.annotations.Param("role") Integer role);
}