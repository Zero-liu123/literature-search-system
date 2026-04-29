package com.example.literaturesearchsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.literaturesearchsystem.entity.Literature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface LiteratureMapper extends BaseMapper<Literature> {

    @Select("SELECT * FROM literature WHERE contributor_id = #{contributorId} ORDER BY create_time DESC")
    List<Literature> selectByContributorId(@Param("contributorId") Long contributorId);

    @Select("SELECT * FROM literature WHERE status = 0 ORDER BY create_time ASC")
    List<Literature> selectPendingList();

    @Update("UPDATE literature SET status = #{status}, review_remark = #{remark}, " +
            "reviewer_id = #{reviewerId}, review_time = NOW() WHERE id = #{id}")
    int updateReviewStatus(@Param("id") Long id,
                           @Param("status") Integer status,
                           @Param("remark") String remark,
                           @Param("reviewerId") Long reviewerId);

    @Update("UPDATE literature SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM literature WHERE contributor_id = #{contributorId} AND status = 1")
    int countApprovedByContributor(@Param("contributorId") Long contributorId);
}