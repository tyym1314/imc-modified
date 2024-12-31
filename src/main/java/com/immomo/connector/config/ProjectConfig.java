package com.immomo.connector.config;

/**
 * Created By wlb on 2019-09-24 11:14
 */
public class ProjectConfig {

  public static final String PROJECT_NAME = "imc-modified";

  public static class Registry {

    public static final String ZK_ADDRESS = "soulchill-b2-zk-001.momo.com:2183,soulchill-b2-zk-002.momo.com:2183,soulchill-b2-zk-003.momo.com:2183";
    public static final String ZK_PATH = "/platform-connector";

  }

  public static class Redis {

//    public static final String CONSUMER_MOMOSTORE = "momostore_soulchill_pubsub_im";
    public static final String CHANNEL_MSG = "platform_im_msg";
    public static final String CHANNEL_KICK = "platform_im_kick";
    public static final String CHANNEL_RECON = "platform_im_reconn";
    public static final String CHANNEL_LINK = "platform_im_link";
    public static final String[] CONSUMER_CHANNELS = {CHANNEL_MSG, CHANNEL_KICK, CHANNEL_RECON,
        CHANNEL_LINK};
  }

}