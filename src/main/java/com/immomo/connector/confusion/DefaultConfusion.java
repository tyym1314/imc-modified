package com.immomo.connector.confusion;

import com.immomo.mcf.util.encrypt.MD5;

/**
 * sauth2授权成功后计算得到的sharedSecrect不直接使⽤，加⼀个如下的简 单的变换： w[momoidt}做MD5得到MD5String(momoid为当前⽤户的momoid，两边的[}是
 * 要加的，例：w[18000008t})，MD5String的后2位加sharedSecrct加MD5String 的前2位⽣成最后使⽤的key
 *
 * @author sun.xusen@immomo.com
 * @date 2015-3-9
 */
public class DefaultConfusion implements IConfusion {

  @Override
  public String confusion(String momoid, String source) {
    String hash = MD5.hash("w[" + momoid + "t}");
    return new StringBuilder().append(hash.substring(hash.length() - 2)).append(source)
        .append(hash.substring(0, 2)).toString();
  }
}
