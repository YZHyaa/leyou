package com.leyou.search.service;

import com.leyou.dto.PageResult;
import com.leyou.pojo.Brand;
import com.leyou.pojo.SpecParam;
import com.leyou.pojo.Spu;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.reponsitory.GoodsRepository;
import com.leyou.search.util.SearchUtil;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Resource
    private GoodsClient goodsClient;

    @Resource
    private SearchUtil searchUtil;

    @Resource
    private BrandClient brandClient;

    @Resource
    private CategoryClient categoryClient;

    @Resource
    private SpecificationClient specificationClient;

    public PageResult<Goods> search(SearchRequest request) {

        String key = request.getKey();
        //如果key为空 不允许搜索
        if(StringUtils.isBlank(key)){
            return null;
        }

        //构建查询条件
        NativeSearchQueryBuilder querybuilder = new NativeSearchQueryBuilder();
        /// 添加查询条件
        //MatchQueryBuilder basicQuery = QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND);
        //querybuilder.withQuery(basicQuery);
        BoolQueryBuilder boolQueryBuilder = buildBooleanQueryBuilder(request);
        querybuilder.withQuery(boolQueryBuilder);
        //通过sourcefilter设置返回字段，只要id,skus,subTitle
        querybuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));
        //分页
        //准备分页参数
        int page = request.getPage();
        int size = request.getSize();
        querybuilder.withPageable(PageRequest.of(page-1,size));
        //排序
        String sortBy = request.getSortBy();
        Boolean desc = request.getDescending();
        if(StringUtils.isNotBlank(sortBy)){
            querybuilder.withSort(SortBuilders.fieldSort(sortBy).order(desc ? SortOrder.DESC : SortOrder.ASC));
        }

        String categoryAggName = "categories";
        String brandAggName = "brands";
        querybuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        querybuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        //执行搜索
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(querybuilder.build());

        //解析聚合结果集
        List<Map<String,Object>> categories = getCategoryAggResult(goodsPage.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation(brandAggName));

        // 判断分类聚合的结果集大小，等于1则聚合
        List<Map<String, Object>> specs = null;
        if (categories.size() == 1) {
            //specs = getParamAggResult((Long)categories.get(0).get("id"), basicQuery);
            specs = getParamAggResult((Long)categories.get(0).get("id"), boolQueryBuilder);
        }
        //查询，获取结果
        //Page<Goods> pageInfo = this.goodsRepository.search(querybuilder.build());
        //封装结果并返回
        //return new PageResult<>(pageInfo.getTotalElements(),pageInfo.getTotalPages(),pageInfo.getContent());

        return new SearchResult(goodsPage.getContent(),goodsPage.getTotalElements(),goodsPage.getTotalPages(),categories,brands,specs);

    }

    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        //处理聚合结果集
        LongTerms terms = (LongTerms) aggregation;
        //获取所有品牌Id
        List<LongTerms.Bucket> buckets = terms.getBuckets();
        //定义一个品牌集合，搜集所有品牌对象
        List<Brand> brands = new ArrayList<>();
        //解析所有的Id桶，查询品牌
        buckets.forEach(bucket -> {
            Brand brand = this.brandClient.queryBrandById(bucket.getKeyAsNumber().longValue());
            brands.add(brand);
        });

        return brands;
    }

    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        //处理局和结果集
        LongTerms terms = (LongTerms) aggregation;
        //获取所有分类的id桶
        List<LongTerms.Bucket> buckets = terms.getBuckets();
        //定义一个分类集合，搜索所有分类
        List<Map<String,Object>> categories = new ArrayList<>();
        List<Long> cids = new ArrayList<>();
        //解析所有id
        buckets.forEach(bucket -> {
            cids.add(bucket.getKeyAsNumber().longValue());
        });
        List<String> names = this.categoryClient.queryNameByIds(cids);
        for(int i=0;i<cids.size();i++){
            Map<String,Object> map = new HashMap<>();
            map.put("id",cids.get(i));
            map.put("name",names.get(i));
            categories.add(map);
        }

        return categories;
    }

    /**
     * 聚合出规格参数过滤条件
     * @param id
     * @param basicQuery
     */
    private List<Map<String,Object>> getParamAggResult(Long id, QueryBuilder basicQuery) {

        // 创建自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 基于基本的查询条件，聚合规格参数
        queryBuilder.withQuery(basicQuery);
        // 查询要聚合的规格参数
        List<SpecParam> params = this.specificationClient.queryParams(null, id, null, true);
        // 添加聚合
        params.forEach(param -> {
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
        });
        // 只需要聚合结果集，不需要查询结果集
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));

        // 执行聚合查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());

        // 定义一个集合，收集聚合结果集
        List<Map<String, Object>> paramMapList = new ArrayList<>();
        // 解析聚合查询的结果
        Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            // 放入规格参数名
            map.put("k", entry.getKey());
            // 收集规格参数值
            List<Object> options = new ArrayList<>();
            // 解析每个聚合
            StringTerms terms = (StringTerms)entry.getValue();
            // 遍历每个聚合中桶，把桶中key放入收集规格参数的集合中
            terms.getBuckets().forEach(bucket -> options.add(bucket.getKeyAsString()));
            map.put("options", options);
            paramMapList.add(map);
        }

        return paramMapList;
    }

    /**
     * 构建bool查询构建器
     * @param request
     * @return
     */
    private BoolQueryBuilder buildBooleanQueryBuilder(SearchRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 添加基本查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));

        // 添加过滤条件
        if (CollectionUtils.isEmpty(request.getFilter())){
            return boolQueryBuilder;
        }
        for (Map.Entry<String,String> entry : request.getFilter().entrySet()) {

            String key = entry.getKey();
            // 如果过滤条件是“品牌”, 过滤的字段名：brandId
            if (StringUtils.equals("品牌", key)) {
                key = "brandId";
            } else if (StringUtils.equals("分类", key)) {
                // 如果是“分类”，过滤字段名：cid3
                key = "cid3";
            } else {
                // 如果是规格参数名，过滤字段名：specs.key.keyword
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }

        return boolQueryBuilder;
    }

    public void createIndex(Long id) throws IOException {

        Spu spu = this.goodsClient.querySpuById(id);
        // 构建商品
        Goods goods = searchUtil.buildGoods(spu);

        // 保存数据到索引库
        this.goodsRepository.save(goods);
    }

    public void deleteIndex(Long id) {
        this.goodsRepository.deleteById(id);
    }
}
