package com.awaitz.base.rocketmq.producer;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.aliyun.openservices.shade.com.alibaba.fastjson.serializer.SerializerFeature;

public abstract class AbstractAwaitzOrderProducer implements AwaitzProducer{

    public abstract OrderProducer getOrderProducer();

    public SendResult orderSend(final ProducerMessage<?> producerMessage, final String shardingKey){
        Message message = new Message(producerMessage.getTopic(), producerMessage.getTag(),
                producerMessage.getMessageKey(), JSON.toJSONBytes(producerMessage.getData(), SerializerFeature.WriteMapNullValue)
        );
        return this.getOrderProducer().send(message,shardingKey);
    }
}
