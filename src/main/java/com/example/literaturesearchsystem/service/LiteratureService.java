package com.example.literaturesearchsystem.service;

import com.example.literaturesearchsystem.dto.LiteratureSearchDTO;
import com.example.literaturesearchsystem.dto.LiteratureVO;
import com.example.literaturesearchsystem.entity.Literature;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface LiteratureService {

    Literature addLiterature(Literature literature, MultipartFile file);

    Literature updateLiterature(Long id, Literature literature, MultipartFile file);

    LiteratureVO getById(Long id);

    void deleteById(Long id);

    void syncToEs();

    // 重要：搜索方法
    Map<String, Object> search(LiteratureSearchDTO searchDTO);
}