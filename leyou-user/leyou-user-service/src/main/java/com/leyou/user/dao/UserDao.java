package com.leyou.user.dao;

import org.springframework.stereotype.Component;
import com.leyou.user.pojo.User;
import tk.mybatis.mapper.common.Mapper;

@org.apache.ibatis.annotations.Mapper
@Component
public interface UserDao extends Mapper<User> {
}
