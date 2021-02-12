package com.leyou.item.dao;

import com.leyou.pojo.Category;
import org.apache.ibatis.annotations.Mapper;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;

@Mapper
public interface CategoryDao extends tk.mybatis.mapper.common.Mapper<Category>, SelectByIdListMapper<Category,Long> {
}
