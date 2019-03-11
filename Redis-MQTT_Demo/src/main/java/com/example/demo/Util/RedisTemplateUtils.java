package com.example.demo.Util;
import java.util.List;
import java.util.Set;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTemplateUtils {


    static final Logger logger = LoggerFactory.getLogger(RedisTemplateUtils.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

//	@Autowired
//	private RedisTemplate<String, Serializable> redisTemplate;

    public static final int LOCK_TIMEOUT = 4;


    // ---------------value 为String 的 操作
    /**
     * set 字符串的 key，value
     *
     * @param key
     * @param value
     */
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 根据key获取字符串的 value
     *
     * @param key
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 根据 key 删除
     *
     * @param key
     */
    public void del(String key) {
        stringRedisTemplate.opsForValue().getOperations().delete(key);
    }


    // ---------------value 为 List 的操作

    /**
     * push 元素到 list中
     */
    public  void  lpush(String key,String value) {
        stringRedisTemplate.opsForList().leftPush(key, value);
    }

    /**
     *  获取 list中 某个下标的元素
     * @param key
     * @param index
     */
    public String  lindex(String key,long index) {
        return stringRedisTemplate.opsForList().index(key, index);
    }


    /**
     * 根据key 获取 list的长度
     * @param key
     * @return
     */
    public long  llen(String key) {
        return stringRedisTemplate.opsForList().size(key);
    }


    /**
     * 根据 key，start，end 获取某一个区间的 list数据集
     * @param key
     * @param start
     * @param end
     * @return
     */
    public  List<String>  lrange(String key, long start, long end){
        return stringRedisTemplate.opsForList().range(key, start, end);
    }


    /**
     * 根据 key值 pattern查询所有匹配的值，比如login*
     * @param key
     * @return
     */
    public  Set<String> keys(String key){
        return  stringRedisTemplate.keys(key);
    }




    /**
     * 加锁
     *
     * @param key   productId - 商品的唯一标志
     * @param value 当前时间+超时时间 也就是时间戳
     * @return
     */
    public boolean lock(String key,String value) {
        if (stringRedisTemplate.opsForValue().setIfAbsent(key, value)) {// 对应setnx命令
            // 可以成功设置,也就是key不存在
            return true;
        }

        // 判断锁超时 - 防止原来的操作异常，没有运行解锁操作 防止死锁
        String currentValue = stringRedisTemplate.opsForValue().get(key);
        // 如果锁过期
        if (!StringUtils.isEmpty(currentValue) && Long.parseLong(currentValue) < System.currentTimeMillis()) {// currentValue不为空且小于当前时间
            // 获取上一个锁的时间value
            String oldValue = stringRedisTemplate.opsForValue().getAndSet(key, value);// 对应getset，如果key存在

            // 假设两个线程同时进来这里，因为key被占用了，而且锁过期了。获取的值currentValue=A(get取的旧的值肯定是一样的),两个线程的value都是B,key都是K.锁时间已经过期了。
            // 而这里面的getAndSet一次只会一个执行，也就是一个执行之后，上一个的value已经变成了B。只有一个线程获取的上一个值会是A，另一个线程拿到的值是B。
            if (!StringUtils.isEmpty(oldValue) && oldValue.equals(currentValue)) {
                // oldValue不为空且oldValue等于currentValue，也就是校验是不是上个对应的商品时间戳，也是防止并发
                return true;
            }
        }
        return false;
    }

    /**
     * 解锁
     *
     * @param key
     * @param value
     */
    public void unlock(String key,String value) {
        try {
            String currentValue = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.isEmpty(currentValue) && currentValue.equals(value)) {
                stringRedisTemplate.opsForValue().getOperations().delete(key);// 删除key
            }

        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
    }

}
