package com.immomo.connector.handler.service.impl;

import com.immomo.connector.handler.service.SauthService;
import com.immomo.moaservice.live.tokencenter.api.ITokenCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created By wlb on 2019-09-19 15:19
 */
@Slf4j
@Service
public class SauthServiceImpl implements SauthService {

  @Autowired
  private ITokenCenterService tokenCenterService;

  @Override
  public boolean auth(String appId, String userId, String userToken) {
    try {
      return tokenCenterService.validateToken(appId, userId, userToken);
    } catch (Exception e) {
      log.error("error validateToken, appId:{}, userId:{}", appId, userId);
      return true;
    }
  }

}