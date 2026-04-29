package com.example.literaturesearchsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.literaturesearchsystem.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {

    /**
     * 获取用户最近的搜索历史（最多20条）
     */
    @Select("SELECT * FROM search_history WHERE user_id = #{userId} ORDER BY search_time DESC LIMIT 20")
    List<SearchHistory> selectByUserId(@Param("userId") Long userId);

    /**
     * 清空用户搜索历史
     */
    @Delete("DELETE FROM search_history WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除用户指定的搜索关键词记录
     */
    @Delete("DELETE FROM search_history WHERE user_id = #{userId} AND keyword = #{keyword}")
    int deleteByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 只保留最近的N条记录，删除更早的
     */
    @Delete("DELETE FROM search_history WHERE user_id = #{userId} AND id NOT IN (" +
            "SELECT id FROM (SELECT id FROM search_history WHERE user_id = #{userId} " +
            "ORDER BY search_time DESC LIMIT #{limit}) t)")
    int keepLatest(@Param("userId") Long userId, @Param("limit") int limit);

}