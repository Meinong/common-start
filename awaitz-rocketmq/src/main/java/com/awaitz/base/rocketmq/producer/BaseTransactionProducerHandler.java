package com.awaitz.base.rocketmq.producer;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.bean.TransactionProducerBean;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.aliyun.openservices.shade.com.alibaba.fastjson.serializer.SerializerFeature;

public abstract class BaseTransactionProducerHandler extends TransactionProducerBean implements AwaitzProducer{
    /**
     * groupId
     */
    private String groupId = "";


    public String getGroupId() {
        return groupId;
    }


    //本地事务执行器
    public abstract LocalTransactionExecuter getLocalTransactionExecuter();

    //check listen
    public abstract LocalTransactionChecker getLocalTransactionChecker();



    public BaseTransactionProducerHandler(String groupId){
        this.groupId = groupId;
    }

    @Override
    public SendResult transactionSend(ProducerMessage<?> producerMessage, Object arg) {
        Message message = new Message(producerMessage.getTopic(), producerMessage.getTag(),
                producerMessage.getMessageKey(), JSON.toJSONBytes(producerMessage.getData(), SerializerFeature.WriteMapNullValue)
        );
        return super.send(message, getLocalTransactionExecuter(), arg);
    }


}
