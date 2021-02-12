package com.leyou.cart.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.interceptor.LoginInteceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.pojo.Sku;
import com.leyou.utils.JsonUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GoodsClient goodsClient;

    private static final String KEY_PREFIX = "user:cart:";

    public void addCart(Cart cart) {

        //获取用户信息
        UserInfo userInfo = LoginInteceptor.getUserInfo();

        //查询购物车记录
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userInfo.getId());

        String key = cart.getSkuId().toString();
        Integer num = cart.getNum();

        //判断当前商品是否已经在购物车中
        if (hashOps.hasKey(key)){
            //在，更新数据
            String cartJson = hashOps.get(key).toString();
            cart = JsonUtils.parse(cartJson, Cart.class);
            cart.setNum(cart.getNum()+num);
        }else {
            //无，新增
            Sku sku = this.goodsClient.querySkuById(cart.getSkuId());
            cart.setUserId(userInfo.getId());
            cart.setPrice(sku.getPrice());
            cart.setTitle(sku.getTitle());
            cart.setOwnSpec(sku.getOwnSpec());
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(),",")[0]);
        }

        //写入redis
        hashOps.put(key,JsonUtils.serialize(cart));

    }

    public List<Cart> queryCarts() {

        UserInfo user = LoginInteceptor.getUserInfo();

        //判断用户是否有购物车记录
        if(!this.redisTemplate.hasKey(KEY_PREFIX+user.getId())){
            return null;
        }
        //获取用户购物车记录
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + user.getId());

        //获取购物车map中所有cart集合
        List<Object> cartJson = hashOps.values();

        //如果购物车集合为空，直接返回null
        if(CollectionUtils.isEmpty(cartJson)){
            return null;
        }

        return cartJson.stream().map(cart -> JsonUtils.parse(cart.toString(),Cart.class)).collect(Collectors.toList());
    }

    public void updateCartNum(Cart cart) {
        // 获取登陆信息
        UserInfo userInfo = LoginInteceptor.getUserInfo();
        String key = KEY_PREFIX + userInfo.getId();
        // 获取hash操作对象
        BoundHashOperations<String, Object, Object> hashOperations = this.redisTemplate.boundHashOps(key);
        // 获取购物车信息
        String cartJson = hashOperations.get(cart.getSkuId().toString()).toString();
        Cart cart1 = JsonUtils.parse(cartJson, Cart.class);
        // 更新数量
        cart1.setNum(cart.getNum());
        // 写入购物车
        hashOperations.put(cart.getSkuId().toString(), JsonUtils.serialize(cart1));
    }

    public void deleteCart(String skuId) {
        // 获取登录用户
        UserInfo user = LoginInteceptor.getUserInfo();
        String key = KEY_PREFIX + user.getId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        hashOps.delete(skuId);
    }
}
