package com.nowcoder.community.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 利用AOP思想去统一管理日志
 */
@Component
@Aspect
public class ServiceLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    //第一个*代表任意返回值
    //第二个*代表任意类
    //第三个*代表任意方法
    //括号里的..代表任意参数
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut() {}

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();


        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

        if(requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            String ip = request.getRemoteHost();
            logger.info(String.format("用户[%s], 在[%s], 访问了[%s]", ip, now, target));
        } else {
            logger.info(String.format("系统, 在[%s], 访问了[%s]", now, target));
        }
    }
}
