package com.immomo.connector.handler.consumer;

import com.immomo.mcf.dao.redis.pubsub.PubSubEventListener;
import com.immomo.momostore.proxy.PubSubDao;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By wlb on 2019-09-24 12:03
 */
@Slf4j
public class RedisSubscriber {

  private final String momostore;
  private final String[] channels;
  private final MessageListener messageListener;

  private PubSubDao pubSubDao;

  public RedisSubscriber(String momostore, String[] channels, MessageListener messageListener) {
    this.momostore = momostore;
    this.channels = channels;
    this.messageListener = messageListener;
  }


  public void start() {
    if (pubSubDao != null) {
      throw new IllegalStateException("RedisSubscriber already started");
    }
    pubSubDao = new PubSubDao(momostore, new MessageAdapter(messageListener));
    pubSubDao.subscribe(channels);
  }

  public void stop() {
    if (pubSubDao != null) {
      try {
        pubSubDao.unsubscribe(channels);
      } catch (Exception e) {
        log.error("error unsubscribe, channels:{}", String.join(",", channels), e);
      }
    }
  }

  private static class MessageAdapter implements PubSubEventListener {

    private final MessageListener messageListener;

    public MessageAdapter(MessageListener messageListener) {
      this.messageListener = messageListener;
    }

    @Override
    public void onMessage(String channel, String message) {
      messageListener.onMessage(channel, message);
    }

    @Override
    public void onPMessage(String s, String s1, String s2) {
    }

    @Override
    public void onSubscribe(String s, int i) {
    }

    @Override
    public void onUnsubscribe(String s, int i) {
    }

    @Override
    public void onPUnsubscribe(String s, int i) {
    }

    @Override
    public void onPSubscribe(String s, int i) {
    }

    @Override
    public void onPong(String s) {
    }
  }


  public interface MessageListener {

    void onMessage(String channel, String message);

  }
}
