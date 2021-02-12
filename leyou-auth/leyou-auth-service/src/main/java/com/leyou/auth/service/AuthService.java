package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Resource
    private JwtProperties prop;

    public String accredit(String username, String password) {
        //根据用户名和密码去查询
        User user = this.userClient.queryUser(username, password);
        //判断user
        if(user == null){
            return null;
        }
        try {
            //生成token
            UserInfo userInfo = new UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            return JwtUtils.generateToken(userInfo,this.prop.getPrivateKey(),this.prop.getExpire());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
