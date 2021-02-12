package com.leyou.cart.controller;

import com.leyou.cart.pojo.Cart;
import com.leyou.cart.service.CartService;
import com.leyou.cart.service.CartService_mongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class CartController {

//    @Autowired
//    private CartService cartService;

    @Autowired
    private CartService_mongo cartService;

    @PostMapping
    public ResponseEntity<Void> addCart(@RequestBody Cart cart){
        this.cartService.addCart(cart);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<Cart>> queryCarts(){
        List<Cart> carts = this.cartService.queryCarts();
        if(CollectionUtils.isEmpty(carts)){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(carts);
    }

    @PutMapping
    public ResponseEntity<Void> updateCartNum(@RequestBody Cart cart){
        this.cartService.updateCartNum(cart);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{skuId}")
    public ResponseEntity<Void> deleteCart(@PathVariable("skuId") String skuId) {
        this.cartService.deleteCart(skuId);
        return ResponseEntity.noContent().build();
    }
}
