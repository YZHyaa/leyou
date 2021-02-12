package com.leyou.sms.listener;

import com.aliyuncs.exceptions.ClientException;
import com.leyou.sms.config.SmsProperties;
import com.leyou.sms.utils.SmsUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
@EnableConfigurationProperties(SmsProperties.class)
public class SmsListener {

    @Autowired
    private SmsUtils smsUtils;

    @Resource
    private SmsProperties prop;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "leyou.sms.queue",durable = "true"),
            exchange = @Exchange(value = "leyou.sms.exchange",ignoreDeclarationExceptions = "true"),
            key = {"sms.verify.code"}
    ))
    public void listenSms(Map<String,String> msg){
        if (msg == null || msg.size() <= 0) {
            // 放弃处理
            return;
        }
        String phone = msg.get("phone");
        String code = msg.get("code");

        if(phone.equals("") || code.equals("")){
            return;
        }

        //发送短信
        try {
            this.smsUtils.sendSms(phone,code,prop.getSignName(),prop.getVerifyCodeTemplate());
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}
