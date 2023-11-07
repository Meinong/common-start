package com.awaitz.base.rocketmq.consumer;

import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;


public abstract class BaseConsumerHandler {
    /**
     * topic 定义
     */
    private String topic = "";
    /**
     * tag 定义
     */
    private String tag = "";
    /**
     * group id 定义
     */
    private String groupId = "";

    public String getTopic() {
        return topic;
    }

    public String getTag() {
        return tag;
    }

    public String getGroupId() {
        return groupId;
    }


    public BaseConsumerHandler(String topic, String tag, String groupId){
        this.topic = topic;
        this.tag = tag;
        this.groupId = groupId;
    }

    public abstract void doConsume(final Message message, final ConsumeContext context);




}
