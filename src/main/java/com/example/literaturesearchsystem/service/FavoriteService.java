package com.example.literaturesearchsystem.service;

import com.example.literaturesearchsystem.entity.Literature;
import java.util.List;

public interface FavoriteService {

    /**
     * 添加收藏
     */
    boolean addFavorite(Long userId, Long literatureId);

    /**
     * 取消收藏
     */
    boolean removeFavorite(Long userId, Long literatureId);

    /**
     * 检查是否已收藏
     */
    boolean isFavorited(Long userId, Long literatureId);

    /**
     * 获取用户收藏的文献列表
     */
    List<Literature> getUserFavorites(Long userId);
}