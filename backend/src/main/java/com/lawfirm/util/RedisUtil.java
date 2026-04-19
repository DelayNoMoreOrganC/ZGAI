package com.lawfirm.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类（仅在Redis可用时启用）
 */
@Component
@ConditionalOnClass(RedisTemplate.class)
public class RedisUtil {

    @Autowired(required = false)  // 设为可选依赖
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        if (redisTemplate == null) return;
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存并设置过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (redisTemplate == null) return;
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        if (redisTemplate == null) return null;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        if (redisTemplate == null) return false;
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     */
    public Long delete(Collection<String> keys) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.delete(keys);
    }

    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        if (redisTemplate == null) return false;
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (redisTemplate == null) return false;
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        if (redisTemplate == null) return -1L;
        return redisTemplate.getExpire(key);
    }

    /**
     * Hash设置
     */
    public void hSet(String key, String hashKey, Object value) {
        if (redisTemplate == null) return;
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * Hash获取
     */
    public Object hGet(String key, String hashKey) {
        if (redisTemplate == null) return null;
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * Hash获取所有
     */
    public Map<Object, Object> hGetAll(String key) {
        if (redisTemplate == null) return null;
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash删除
     */
    public Long hDelete(String key, Object... hashKeys) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * Set添加
     */
    public Long sAdd(String key, Object... values) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * Set获取所有
     */
    public Set<Object> sMembers(String key) {
        if (redisTemplate == null) return null;
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * Set判断是否存在
     */
    public Boolean sIsMember(String key, Object value) {
        if (redisTemplate == null) return false;
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * List左侧插入
     */
    public Long lLeftPush(String key, Object value) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * List右侧插入
     */
    public Long lRightPush(String key, Object value) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * List获取范围
     */
    public List<Object> lRange(String key, long start, long end) {
        if (redisTemplate == null) return null;
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * List获取大小
     */
    public Long lSize(String key) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 自增
     */
    public Long increment(String key) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增指定值
     */
    public Long increment(String key, long delta) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自减
     */
    public Long decrement(String key) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 自减指定值
     */
    public Long decrement(String key, long delta) {
        if (redisTemplate == null) return 0L;
        return redisTemplate.opsForValue().decrement(key, delta);
    }
}
