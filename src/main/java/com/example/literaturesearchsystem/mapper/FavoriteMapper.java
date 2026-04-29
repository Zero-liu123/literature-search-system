package com.example.literaturesearchsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.literaturesearchsystem.entity.Favorite;
import com.example.literaturesearchsystem.entity.Literature;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT l.* FROM literature l " +
            "INNER JOIN favorite f ON l.id = f.literature_id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC")
    List<Literature> selectFavoritesByUserId(@Param("userId") Long userId);

    // 添加这个方法
    @Select("SELECT literature_id FROM favorite WHERE user_id = #{userId}")
    List<Long> selectLiteratureIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId} AND literature_id = #{literatureId}")
    int exists(@Param("userId") Long userId, @Param("literatureId") Long literatureId);

    @Delete("DELETE FROM favorite WHERE user_id = #{userId} AND literature_id = #{literatureId}")
    int delete(@Param("userId") Long userId, @Param("literatureId") Long literatureId);
}