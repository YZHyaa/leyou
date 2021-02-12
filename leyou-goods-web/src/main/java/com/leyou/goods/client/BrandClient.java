package com.leyou.goods.client;

import com.leyou.api.BrandApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

@FeignClient("item-service")
@Component
public interface BrandClient extends BrandApi {
}