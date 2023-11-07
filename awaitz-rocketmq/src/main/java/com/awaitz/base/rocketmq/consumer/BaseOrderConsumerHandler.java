package com.awaitz.base.rocketmq.consumer;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.order.ConsumeOrderContext;


public abstract class BaseOrderConsumerHandler {
    /**
     * topic 定义
     */
    private String topic = "";
    /**
     * tag 定义
     */
    private String tag = "";

    public String getTopic() {
        return topic;
    }

    public String getTag() {
        return tag;
    }

    public String getGroupId() {
        return groupId;
    }

    /**
     * group id 定义
     */
    private String groupId = "";

    public BaseOrderConsumerHandler(String topic, String tag, String groupId){
        this.topic = topic;
        this.tag = tag;
        this.groupId = groupId;
    }



    public abstract void doConsume(final Message message, final ConsumeOrderContext context);




}
