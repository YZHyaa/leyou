package com.leyou.item.service;

import com.leyou.item.dao.CategoryDao;
import com.leyou.pojo.Category;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Resource
    private CategoryDao categoryDao;

    public List<Category> queryCateByPid(Long pid) {

        Category record = new Category();
        record.setParentId(pid);
        return categoryDao.select(record);
    }

    public List<String> queryNamesByIds(List<Long> ids) {

        List<Category> categories = this.categoryDao.selectByIdList(ids);
        return categories.stream().map(category -> category.getName()).collect(Collectors.toList());
    }

    public List<Category> queryAllByCid3(Long id) {
        Category c3 = this.categoryDao.selectByPrimaryKey(id);
        Category c2 = this.categoryDao.selectByPrimaryKey(c3.getParentId());
        Category c1 = this.categoryDao.selectByPrimaryKey(c2.getParentId());
        return Arrays.asList(c1,c2,c3);
    }
}
