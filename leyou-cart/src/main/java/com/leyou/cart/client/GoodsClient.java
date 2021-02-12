package com.leyou.cart.client;

import com.leyou.api.GoodsApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

@FeignClient("item-service")
@Component
public interface GoodsClient extends GoodsApi{
}
