package com.awaitz.base.rocketmq.consumer;


import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import org.springframework.stereotype.Component;

@Component
public class TestHandler extends BaseConsumerHandler{


    private static final String topic = "topic";
    private static final String tag = "tag";
    private static final String groupId = "groupId";

    public TestHandler() {
        super(topic, tag, groupId);
    }

    @Override
    public void doConsume(Message message, ConsumeContext context) {

        //处理自己业务
        try{

        }catch (Exception e){
            throw e;
        }
    }
}
