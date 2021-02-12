package com.leyou.cart.interceptor;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.cart.config.JwtProperties;
import com.leyou.utils.CookieUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@EnableConfigurationProperties(JwtProperties.class)
@Component
public class LoginInteceptor extends HandlerInterceptorAdapter {

    @Resource
    private JwtProperties jwtProperties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler) throws Exception{
        //获取cookie中的token
        String cookieValue = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());
        //解析token
        UserInfo user = JwtUtils.getInfoFromToken(cookieValue, this.jwtProperties.getPublicKey());
        if(user == null){
            return false;
        }
        //把userinfo放入线程局部变量
        THREAD_LOCAL.set(user);
        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    //清空线程的局部变量
    //必须清空因为使用的tomcat线程池，不手动释放就无法释放
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
