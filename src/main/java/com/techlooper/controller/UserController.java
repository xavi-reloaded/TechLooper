package com.techlooper.controller;

import com.techlooper.model.SocialProvider;
import com.techlooper.model.SocialRequest;
import com.techlooper.model.UserImportData;
import com.techlooper.model.UserInfo;
import com.techlooper.service.SocialService;
import com.techlooper.service.UserImportDataProcessor;
import com.techlooper.service.UserService;
import com.techlooper.util.EmailValidator;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.util.text.TextEncryptor;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Iterator;
import java.util.List;

/**
 * Created by phuonghqh on 12/23/14.
 */
@Controller
public class UserController {

  @Resource
  private ApplicationContext applicationContext;

  @Resource
  private UserService userService;

  @Resource
  private TextEncryptor textEncryptor;

  @ResponseBody
  @RequestMapping(value = "/api/users/add", method = RequestMethod.POST)
  public void save(@RequestBody UserImportData userImportData, HttpServletResponse httpServletResponse) {
    httpServletResponse.setStatus(userService.addCrawledUser(userImportData) ?
            HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_ACCEPTABLE);
  }

  @ResponseBody
  @RequestMapping(value = "/api/users/addAll", method = RequestMethod.POST)
  public void saveAll(@RequestBody List<UserImportData> users, HttpServletResponse httpServletResponse) {
    if (!users.isEmpty()) {
      SocialProvider provider = users.get(0).getCrawlerSource();
      UserImportDataProcessor dataProcessor = applicationContext.getBean(provider + "UserImportDataProcessor", UserImportDataProcessor.class);
      // process raw user data before import into ElasticSearch
      dataProcessor.process(users);
      httpServletResponse.setStatus(userService.addCrawledUserAll(users) > 0 ?
              HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_ACCEPTABLE);
    } else {
      httpServletResponse.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }
  }

  @ResponseBody
  @RequestMapping(value = "/user/save", method = RequestMethod.POST)
  public List<FieldError> save(@RequestBody @Valid UserInfo userInfo, BindingResult result, HttpServletResponse httpServletResponse) {
    if (result.getFieldErrorCount() > 0) {
      httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } else {
      userService.registerVietnamworksAccount(userInfo);
      userService.save(userInfo);
    }
    return result.getFieldErrors();
  }


  @SendToUser("/queue/info")
  @MessageMapping("/user/findByKey")
  @ResponseBody
  @RequestMapping(value = "/user/findByKey", method = RequestMethod.POST)
  public UserInfo getUserInfo(@CookieValue("techlooper.key") String techlooperKey/*, @DestinationVariable String username */) {
    UserInfo userInfo = userService.findUserInfoByKey(techlooperKey);
    userInfo.getLoginSource();
    return userInfo;
  }

  @ResponseBody
  @RequestMapping(value = "/user/verifyUserLogin", method = RequestMethod.POST)
  public void verifyUserLogin(@RequestBody SocialRequest searchRequest, @CookieValue("techlooper.key") String techlooperKey, HttpServletResponse httpServletResponse) {
    if (!textEncryptor.encrypt(searchRequest.getEmailAddress()).equals(techlooperKey)) {
      httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
  }

}
