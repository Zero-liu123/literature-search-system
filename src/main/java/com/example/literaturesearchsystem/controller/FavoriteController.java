package com.example.literaturesearchsystem.controller;

import com.example.literaturesearchsystem.common.Result;
import com.example.literaturesearchsystem.entity.Favorite;
import com.example.literaturesearchsystem.entity.Literature;
import com.example.literaturesearchsystem.mapper.FavoriteMapper;
import com.example.literaturesearchsystem.mapper.LiteratureMapper;
import com.example.literaturesearchsystem.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/literature")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;
    private final LiteratureMapper literatureMapper;
    private final JwtUtil jwtUtil;

    /**
     * 获取当前用户的收藏列表
     */
    @GetMapping("/favorites")
    public Result<List<Literature>> getFavorites(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<Literature> favorites = favoriteMapper.selectFavoritesByUserId(userId);
        return Result.success(favorites);
    }

    /**
     * 添加收藏
     */
    @PostMapping("/favorite/{literatureId}")
    public Result<Void> addFavorite(@PathVariable Long literatureId,
                                    @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 检查文献是否存在
        Literature literature = literatureMapper.selectById(literatureId);
        if (literature == null) {
            return Result.error("文献不存在");
        }

        // 检查是否已收藏
        if (favoriteMapper.exists(userId, literatureId) > 0) {
            return Result.error("已经收藏过了");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setLiteratureId(literatureId);
        favoriteMapper.insert(favorite);

        return Result.success(null);
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/favorite/{literatureId}")
    public Result<Void> removeFavorite(@PathVariable Long literatureId,
                                       @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        favoriteMapper.delete(userId, literatureId);
        return Result.success(null);
    }

    private Long getUserIdFromToken(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserId(token);
    }
}