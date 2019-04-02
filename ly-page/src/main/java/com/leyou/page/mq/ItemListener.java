package com.leyou.page.mq;

import com.leyou.page.service.PageService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ItemListener {

    @Autowired
    private PageService pageService;

    /**
     * 新增或修改
     * @param spuId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "page.item.insert.queue",durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange",type = ExchangeTypes.TOPIC),
            key = {"item.insert","item.update"}

    ))
    public void listenInsertOrUpdate(Long spuId){
        if (spuId == null){
            return ;
        }
        //处理消息，对静态页创建
        pageService.createHtml(spuId);
    }

    /**
     * 删除
     * @param spuId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "page.item.delete.queue",durable = "true"),
            exchange = @Exchange(name = "ly.item.exchange",type = ExchangeTypes.TOPIC),
            key = {"item.delete"}

    ))
    public void listenDelete(Long spuId){
        if (spuId == null){
            return ;
        }
        //处理消息，对静态页删除
        pageService.deleteHtml(spuId);
    }

}
