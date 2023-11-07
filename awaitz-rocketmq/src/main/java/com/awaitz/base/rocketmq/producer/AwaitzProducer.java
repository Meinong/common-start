package com.awaitz.base.rocketmq.producer;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.SendCallback;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter;

public interface AwaitzProducer {

    default SendResult commonSend(ProducerMessage<?> producerMessage) {
        return null;
    }


    default void sendAsync(ProducerMessage<?> producerMessage, SendCallback sendCallback){

    }

    //延迟消息
    default SendResult delaySend(ProducerMessage<?> producerMessage,Long millisecond){
        return null;
    }

    //延迟定点消息
    default SendResult delayFixedPointSend(ProducerMessage<?> producerMessage,String time) {
        return null;
    }


    /**
     * 发送顺序消息
     *
     * @param producerMessage 消息
     * @param shardingKey 顺序消息选择因子，发送方法基于shardingKey选择具体的消息队列
     * @return {@link SendResult} 消息发送结果，含消息Id
     */
    default SendResult orderSend(final ProducerMessage<?> producerMessage, final String shardingKey){
        return null;
    }


    //发送事务型消息
    default SendResult transactionSend(final ProducerMessage<?> producerMessage, Object arg) {
        return null;
    }



}
