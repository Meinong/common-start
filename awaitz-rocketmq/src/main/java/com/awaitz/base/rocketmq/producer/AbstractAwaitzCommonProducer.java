package com.awaitz.base.rocketmq.producer;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.SendCallback;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.aliyun.openservices.shade.com.alibaba.fastjson.serializer.SerializerFeature;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public abstract class AbstractAwaitzCommonProducer implements AwaitzProducer{

    public abstract Producer getProducer();

    public SendResult commonSend(ProducerMessage<?> producerMessage){
        Message message = new Message(producerMessage.getTopic(), producerMessage.getTag(),
                producerMessage.getMessageKey(), JSON.toJSONBytes(producerMessage.getData(), SerializerFeature.WriteMapNullValue)
        );
        return this.getProducer().send(message);
    }


    public void sendAsync(ProducerMessage<?> producerMessage, SendCallback sendCallback){
        Message message = new Message(producerMessage.getTopic(), producerMessage.getTag(),
                producerMessage.getMessageKey(), JSON.toJSONBytes(producerMessage.getData(),SerializerFeature.WriteMapNullValue)
        );
        this.getProducer().sendAsync(message,sendCallback);
    }

    //延迟消息
    public SendResult delaySend(ProducerMessage<?> producerMessage,Long millisecond){
        Message message = new Message(producerMessage.getTopic(), producerMessage.getTag(),
                producerMessage.getMessageKey(), JSON.toJSONBytes(producerMessage.getData(), SerializerFeature.WriteMapNullValue)
        );
        message.setStartDeliverTime(System.currentTimeMillis() + millisecond);
        return this.getProducer().send(message);
    }

    //延迟定点消息
    public SendResult delayFixedPointSend(ProducerMessage<?> producerMessage,String time)  {
        Message message = new Message(producerMessage.getTopic(), producerMessage.getTag(),
                producerMessage.getMessageKey(), JSON.toJSONBytes(producerMessage.getData(), SerializerFeature.WriteMapNullValue)
        );
        try {
            message.setStartDeliverTime(
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime()
            );
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return this.getProducer().send(message);
    }
}
