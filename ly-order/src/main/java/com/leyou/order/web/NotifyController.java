package com.leyou.order.web;

import com.leyou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("notify")
@Slf4j
public class NotifyController {

    @Autowired
    private OrderService orderService;

    @PostMapping(value = "wxpay",produces = "application/xml")
    public Map<String, String> hello(@RequestBody Map<String,String> result){
        //处理回调
        orderService.handleNotify(result);

        log.info("[支付回调] 接受微信支付回调,结果{}",result);
        //返回成功
        Map<String,String> msg = new HashMap<>();
        msg.put("return_code","SUCCESS");
        msg.put("return_msg","OK");

        return msg;
    }
}
