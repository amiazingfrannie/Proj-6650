package SkiConsumer;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.time.Duration;

public class RedisPoolManager {
  private static final JedisPoolConfig poolConfig = buildPoolConfig();
  private static final JedisPool jedisPool = new JedisPool(poolConfig, "18.207.120.52", 6379, 2000, "password");

  private static JedisPoolConfig buildPoolConfig() {
    final JedisPoolConfig config = new JedisPoolConfig();
    config.setMaxTotal(128);
    config.setMaxIdle(128);
    config.setMinIdle(16);
    config.setTestOnBorrow(true);
    config.setTestOnReturn(true);
    config.setTestWhileIdle(true);
    config.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
    config.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
    config.setNumTestsPerEvictionRun(3);
    config.setBlockWhenExhausted(true);
    return config;
  }

  public static JedisPool getPool() {
    return jedisPool;
  }

  public static void closePool() {
    if (jedisPool != null) {
      jedisPool.close();
    }
  }
}
