package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.dto.PageResult;
import com.leyou.item.dao.BrandDao;
import com.leyou.pojo.Brand;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.List;

@Service
public class BrandService {

    @Resource
    BrandDao brandDao;

    public PageResult queryBrandByPage(Integer page, Integer rows, String key, String sortBy, Boolean desc) {

        Example example = new Example(Brand.class);
        Example.Criteria criteria = example.createCriteria();

        if(StringUtils.isNotBlank(key)){
            criteria.andLike("name","%"+key+"%").orEqualTo("letter",key);
        }
        if(StringUtils.isNotBlank(sortBy)){
            example.setOrderByClause(sortBy+"  "+(desc ? "desc" : "asc"));
        }
        PageHelper.startPage(page,rows);
        List<Brand> brands = brandDao.selectByExample(example);

        PageInfo<Brand> pageInfo = new PageInfo<>(brands);
        return new PageResult<>(pageInfo.getTotal(),pageInfo.getPages(),pageInfo.getList());

    }

//    @Transactional
//    public void saveBrand(BrandIn brandIn) {
//
//        Brand brand = new Brand();
//        BeanUtils.copyProperties(brandIn,brand);
//        List<Long> cids = brandIn.getCids();
//        //新增品牌
//        brandDao.insertSelective(brand);
//        //新增中间表
//        cids.forEach(cid->{
//            brandDao.insertCategoryAndBrand(cid,brand.getId());
//        });
//    }

    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        // 先新增brand
        this.brandDao.insertSelective(brand);

        // 在新增中间表
        cids.forEach(cid -> {
            this.brandDao.insertCategoryAndBrand(cid, brand.getId());
        });
    }

    public List<Brand> queryBrandsByCid(Long cid) {

        return this.brandDao.selectBrandByCid(cid);
    }

    public Brand queryBrandById(Long id) {
        return this.brandDao.selectByPrimaryKey(id);
    }
}
