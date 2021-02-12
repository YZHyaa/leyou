package com.leyou.cart.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.interceptor.LoginInteceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.pojo.Sku;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class CartService_mongo {

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private MongoTemplate mongoTemplate;


    public void addCart(Cart cart) {

        //获取用户信息
        UserInfo userInfo = LoginInteceptor.getUserInfo();

        Query query = new Query(Criteria.where("userId").is(userInfo.getId()).and("skuId").is(cart.getSkuId()));
        if(this.mongoTemplate.count(query,Cart.class) != 0){
            Update update = new Update().set("num",cart.getNum());
            this.mongoTemplate.updateFirst(query,update,Cart.class);
        }else {
            Sku sku = this.goodsClient.querySkuById(cart.getSkuId());
            cart.setUserId(userInfo.getId());
            cart.setPrice(sku.getPrice());
            cart.setTitle(sku.getTitle());
            cart.setOwnSpec(sku.getOwnSpec());
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(),",")[0]);
            this.mongoTemplate.save(cart);
        }

    }

    public List<Cart> queryCarts() {

        UserInfo user = LoginInteceptor.getUserInfo();

        Query query = new Query(Criteria.where("userId").is(user.getId()));
        List<Cart> carts = this.mongoTemplate.find(query, Cart.class);
        if(CollectionUtils.isEmpty(carts)){
            return null;
        }

        return carts;
    }

    public void updateCartNum(Cart cart) {
        // 获取登陆信息
        UserInfo userInfo = LoginInteceptor.getUserInfo();

        Query query = new Query(Criteria.where("userId").is(userInfo.getId()).and("skuId").is(cart.getSkuId()));
        Update update = new Update().set("num",cart.getNum());
        this.mongoTemplate.updateFirst(query,update,Cart.class);
    }

    public void deleteCart(String skuId) {
        // 获取登录用户
        UserInfo user = LoginInteceptor.getUserInfo();
        Query query = new Query(Criteria.where("userId").is(user.getId()).and("skuId").is(Long.parseLong(skuId)));
        this.mongoTemplate.remove(query,Cart.class);
    }
}
