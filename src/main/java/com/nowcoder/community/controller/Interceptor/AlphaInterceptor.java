package com.nowcoder.community.controller.Interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
- 在请求开始时查询登录用户
- 在本次请求中持有用户数据
- 在模板视图上显示用户数据
- 在请求结束时清理用户数据
 */

@Component
public class AlphaInterceptor implements HandlerInterceptor {


    private static final Logger logger = LoggerFactory.getLogger(AlphaInterceptor.class);

    //在Controller之前执行，如果返回false，就要取消请求，Controller不再往后执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("preHandler: " + handler.toString());
        return true;
    }

    //Controller后执行
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        logger.debug("postHandler: " + handler.toString());
    }

    //在TemplateEngine之后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        logger.debug("afterCompletion: " + handler.toString());
    }
}
