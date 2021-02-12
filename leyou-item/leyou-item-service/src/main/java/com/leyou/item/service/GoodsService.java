package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.dto.PageResult;
import com.leyou.dto.SpuBo;
import com.leyou.item.dao.*;
import com.leyou.pojo.Sku;
import com.leyou.pojo.Spu;
import com.leyou.pojo.SpuDetail;
import com.leyou.pojo.Stock;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Resource
    private SpuDao spuDao;

    @Resource
    private BrandDao brandDao;

    @Resource
    private CategoryService categoryService;

    @Resource
    private StockDao stockDao;

    @Resource
    private SkuDao skuDao;

    @Resource
    private SpuDetailDao spuDetailDao;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public static final Logger LOGGER = LoggerFactory.getLogger(GoodsService.class);

    public PageResult<SpuBo> querySpuBoByPage(Integer page, Integer rows, String key, Boolean saleable) {

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();

        if(StringUtils.isNotBlank(key))
        criteria.andLike("title","%"+key+"%");
        if(saleable != null){
            criteria.andEqualTo("saleable",saleable);
        }

        PageHelper.startPage(page,rows);
        List<Spu> spus = spuDao.selectByExample(example);
        PageInfo pageInfo = new PageInfo(spus);

        List<SpuBo> spuBos = new ArrayList<>();
        spus.forEach(spu -> {
            SpuBo spuBo = new SpuBo();
            BeanUtils.copyProperties(spu,spuBo);
            List<String> names = this.categoryService.queryNamesByIds(Arrays.asList(spu.getCid1(),spu.getCid2(),spu.getCid3()));
            spuBo.setCname(StringUtils.join( names, '/'));
            spuBo.setBname((this.brandDao.selectByPrimaryKey(spu.getBrandId())).getName());
            spuBos.add(spuBo);
        });



        return new PageResult<>(pageInfo.getTotal(),pageInfo.getPages(),spuBos);
    }

    /**
     * 新增商品
     * @param spuBo
     */
    @Transactional
    public void saveGoods(SpuBo spuBo) {
        // 新增spu
        // 设置默认字段
        spuBo.setId(null);
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        this.spuDao.insertSelective(spuBo);

        // 新增spuDetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        this.spuDetailDao.insertSelective(spuDetail);

        saveSkuAndStock(spuBo);

        sendMessage(spuBo.getId(),"insert");
    }

    private void saveSkuAndStock(SpuBo spuBo) {
        spuBo.getSkus().forEach(sku -> {
            // 新增sku
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuDao.insertSelective(sku);

            // 新增库存
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            this.stockDao.insertSelective(stock);
        });
    }

    /**
     * 根据spuId查询spuDetail
     * @param spuId
     * @return
     */
    public SpuDetail querySpuDetailBySpuId(Long spuId) {

        return this.spuDetailDao.selectByPrimaryKey(spuId);
    }

    /**
     * 根据spuId查询sku的集合
     * @param spuId
     * @return
     */
    public List<Sku> querySkusBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skus = this.skuDao.select(sku);
        skus.forEach(s -> {
            Stock stock = this.stockDao.selectByPrimaryKey(s.getId());
            s.setStock(stock.getStock());
        });
        return skus;
    }

    @Transactional
    public void updateGoods(SpuBo spu) {
        // 查询以前sku
        List<Sku> skus = this.querySkusBySpuId(spu.getId());
        // 如果以前存在，则删除
        if(!CollectionUtils.isEmpty(skus)) {
            List<Long> ids = skus.stream().map(s -> s.getId()).collect(Collectors.toList());
            // 删除以前库存
            Example example = new Example(Stock.class);
            example.createCriteria().andIn("skuId", ids);
            this.stockDao.deleteByExample(example);

            // 删除以前的sku
            Sku record = new Sku();
            record.setSpuId(spu.getId());
            this.skuDao.delete(record);

        }
        // 新增sku和库存
        saveSkuAndStock(spu);

        // 更新spu
        spu.setLastUpdateTime(new Date());
//        spu.setCreateTime(null);
        spu.setValid(null);
        spu.setSaleable(null);
        this.spuDao.updateByPrimaryKeySelective(spu);

        // 更新spu详情
        this.spuDetailDao.updateByPrimaryKeySelective(spu.getSpuDetail());

        sendMessage(spu.getId(),"update");
    }

    public Spu querySpuById(Long id) {
        return this.spuDao.selectByPrimaryKey(id);
    }

    private void sendMessage(Long id, String type){
        // 发送消息
        try {
            this.amqpTemplate.convertAndSend("item." + type, id);
        } catch (Exception e) {
            LOGGER.error("{}商品消息发送异常，商品id：{}", type, id, e);
        }
    }

    public Sku querySkuById(Long id) {
        return this.skuDao.selectByPrimaryKey(id);
    }
}
