package com.example.literaturesearchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.literaturesearchsystem.entity.Favorite;
import com.example.literaturesearchsystem.entity.Literature;
import com.example.literaturesearchsystem.mapper.FavoriteMapper;
import com.example.literaturesearchsystem.mapper.LiteratureMapper;
import com.example.literaturesearchsystem.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final LiteratureMapper literatureMapper;

    @Override
    public boolean addFavorite(Long userId, Long literatureId) {
        // 检查是否已收藏
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getLiteratureId, literatureId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return false;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setLiteratureId(literatureId);
        favoriteMapper.insert(favorite);
        log.info("用户 {} 收藏文献 {}", userId, literatureId);
        return true;
    }

    @Override
    public boolean removeFavorite(Long userId, Long literatureId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getLiteratureId, literatureId);
        int result = favoriteMapper.delete(wrapper);
        log.info("用户 {} 取消收藏文献 {}", userId, literatureId);
        return result > 0;
    }

    @Override
    public boolean isFavorited(Long userId, Long literatureId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getLiteratureId, literatureId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Literature> getUserFavorites(Long userId) {
        List<Long> literatureIds = favoriteMapper.selectLiteratureIdsByUserId(userId);
        if (literatureIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Literature> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Literature::getId, literatureIds);
        wrapper.orderByDesc(Literature::getUpdateTime);
        return literatureMapper.selectList(wrapper);
    }
}