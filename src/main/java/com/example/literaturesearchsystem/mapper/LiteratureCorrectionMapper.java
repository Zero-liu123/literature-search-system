package com.example.literaturesearchsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.literaturesearchsystem.entity.LiteratureCorrection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LiteratureCorrectionMapper extends BaseMapper<LiteratureCorrection> {

    @Select("SELECT * FROM literature_correction WHERE contributor_id = #{contributorId} ORDER BY create_time DESC")
    List<LiteratureCorrection> selectByContributorId(@Param("contributorId") Long contributorId);

    @Select("SELECT * FROM literature_correction WHERE literature_id = #{literatureId} AND status = 0")
    List<LiteratureCorrection> selectPendingByLiteratureId(@Param("literatureId") Long literatureId);
}