package com.server.redis;

import com.server.domain.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁类  基于redis的SETNX 和 SETEX
 * @Author gg.rao
 * @Date 2019/4/4 15:35
 */
@Component
public class DistributedLockHandler {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockHandler.class);
    //单个业务持有锁的时间30s，防止死锁
    private final static long LOCK_EXPIRE = 30 * 1000L;
    //默认30ms尝试一次
    private final static long LOCK_TRY_INTERVAL = 30L;
    //默认尝试20s
    private final static long LOCK_TRY_TIMEOUT = 20 * 1000L;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * @Description 尝试获取锁
     * @Date 2019/4/4 15:38
     * @Param [lock] lock 锁的名称
     * @return boolean
     **/
    public boolean tryLock(Lock lock){
        logger.info("add lock...");
        return getLock(lock,LOCK_TRY_TIMEOUT,LOCK_TRY_INTERVAL,LOCK_EXPIRE);
    }

    /**
     * 尝试获取全局锁
     *
     * @param lock    锁的名称
     * @param timeout 获取超时时间 单位ms
     * @return true 获取成功，false获取失败
     */
    public boolean tryLock(Lock lock, long timeout) {
        return getLock(lock, timeout, LOCK_TRY_INTERVAL, LOCK_EXPIRE);
    }

    /**
     * 操作redis获取全局锁
     *
     * @param lock           锁的名称
     * @param timeout        获取的超时时间
     * @param tryInterval    多少ms尝试一次
     * @param lockExpireTime 获取成功后锁的过期时间
     * @return true 获取成功，false获取失败
     */
    public boolean getLock(Lock lock,long timeout,long tryInterval,long lockExpireTime){
        try {
            if (StringUtils.isEmpty(lock.getName()) || StringUtils.isEmpty(lock.getValue())) {
                return false;
            }
            long startTime = System.currentTimeMillis();
            do {
                //如果lock不存在
                if (!stringRedisTemplate.hasKey(lock.getName())) {
                    ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
                    ops.set(lock.getName(), lock.getValue(), lockExpireTime, TimeUnit.MILLISECONDS);
                    return true;
                } else {
                    //存在锁
                    logger.info("lock is exists!");
                }
                //尝试超过了设定值之后直接跳出循环
                if (System.currentTimeMillis() - startTime > timeout) {
                    return false;
                }
                Thread.sleep(tryInterval);
            } while (stringRedisTemplate.hasKey(lock.getName()));
        }catch (Exception e){
            logger.error(e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * 释放锁
     */
    public void releaseLock(Lock lock) {
        if (!StringUtils.isEmpty(lock.getName())) {
            stringRedisTemplate.delete(lock.getName());
        }
    }
}
