package SkiConsumer;

import redis.clients.jedis.Jedis;

public class RedisClient {
  private static Jedis jedis;

  public static void connect(String host, int port) {
    jedis = new Jedis(host, port);
  }

  public static Jedis getClient() {
    return jedis;
  }
}
