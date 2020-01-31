package edu.ozu.drone.client.world;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.function.BiConsumer;

class RedisListener
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RedisListener.class);

    private BiConsumer<String, String> callback;
    private redis.clients.jedis.Jedis jedis;
    private String host, wid;
    private Listener listener;

    public RedisListener(String host, String wid, BiConsumer<String, String> callback)
    {
        this.host = host;
        this.wid = wid;
        this.callback = callback;
    }

    void run()
    {
        Thread pubsub = new Thread(() -> {
            jedis = new Jedis(host);
            try {
                listener = new Listener();
                jedis.configSet("notify-keyspace-events", "KEA"); // all key events
                jedis.psubscribe(listener, "__key*__:" + wid); // changes to world key
            } catch (Exception e) {
                logger.error("Excepction on jedis pubsub : " + e.getMessage());
                e.printStackTrace();
            }
        });
        pubsub.setDaemon(true);
        pubsub.start();
    }

    void close()
    {
        listener.punsubscribe();
        jedis.close();
    }

    class Listener extends JedisPubSub
    {
        @Override
        public void onPSubscribe(String pattern, int channels)
        {
            super.onPSubscribe(pattern, channels);

            callback.accept("subscribing " + pattern, String.valueOf(channels));
        }

        @Override
        public void onPUnsubscribe(String pattern, int channels)
        {
            super.onPUnsubscribe(pattern, channels);

            callback.accept("unsubscribe " + pattern, String.valueOf(channels));
        }

        @Override
        public void onMessage(String channel, String message)
        {
            super.onMessage(channel, message);

            callback.accept(channel, message);
        }

        @Override
        public void onPMessage(String pattern, String channel, String message)
        {
            super.onPMessage(pattern, channel, message);

            callback.accept(channel, message);
        }
    }
}
