package com.leyou.goods.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils {

    private static final ExecutorService es = Executors.newFixedThreadPool(10);

    public static void execute(Runnable runnable) {
        es.submit(runnable);
    }

//    location /item {
//			# 先找本地
//        root html;
//        if (!-f $request_filename) { #请求的文件不存在，就反向代理
//            proxy_pass http://127.0.0.1:8084;
//            break;
//        }
//    }
}