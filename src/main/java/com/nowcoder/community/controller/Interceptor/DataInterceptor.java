package com.nowcoder.community.controller.Interceptor;


import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.AdvancedDataService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private AdvancedDataService dataService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        dataService.recordUV(request.getRemoteHost());

        User user = hostHolder.getUser();
        if(user != null) {
            dataService.recordDAU(user.getId());
        }
        return true;
    }
}
