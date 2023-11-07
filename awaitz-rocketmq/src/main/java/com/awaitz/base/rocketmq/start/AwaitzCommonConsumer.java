package com.awaitz.base.rocketmq.start;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ExpressionType;
import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.bean.ConsumerBean;
import com.aliyun.openservices.ons.api.bean.Subscription;
import com.awaitz.base.rocketmq.config.MqConfig;
import com.awaitz.base.rocketmq.consumer.BaseConsumerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;


public class AwaitzCommonConsumer {

    @Autowired(required = false)
    private List<BaseConsumerHandler> baseConsumerHandlerList;
    @Autowired
    private MqConfig mqConfig;
    private List<ConsumerBean> consumerBeans;

    @PostConstruct
    public synchronized void init(){
        List<ConsumerBean> consumerBeans = buildConsumerStartInfo();
        this.setConsumerBeans(consumerBeans);
        start();
    }

    @PreDestroy
    public synchronized void destroy(){
        shutdown();
    }


    private void start(){
        for (ConsumerBean consumerBean : consumerBeans){
             consumerBean.start();
        }
    }

    private void shutdown(){
        for (ConsumerBean consumerBean : consumerBeans){
            consumerBean.shutdown();
        }
    }

    public void setConsumerBeans(List<ConsumerBean> consumerBeans) {
        this.consumerBeans = consumerBeans;
    }



    private List<ConsumerBean> buildConsumerStartInfo() {

        //将相同的GroupId 组合成一个ConsumerBean
        List<ConsumerBean> consumerBeans = new ArrayList<>();
        if(CollectionUtils.isEmpty(baseConsumerHandlerList)){
            return consumerBeans;
        }

        //相同的GroupId 组合在一起
        Map<String,List<BaseConsumerHandler>> handlerMap =
                baseConsumerHandlerList.stream().collect(Collectors.groupingBy(BaseConsumerHandler::getGroupId));

        for (Map.Entry<String,List<BaseConsumerHandler>> mapList: handlerMap.entrySet()){
            String groupId = mapList.getKey();
            List<BaseConsumerHandler> baseConsumerHandlers = mapList.getValue();
            ConsumerBean consumerBean = new ConsumerBean();
            Properties properties = mqConfig.getMqProperties();
            properties.setProperty(PropertyKeyConst.GROUP_ID, groupId);
            consumerBean.setProperties(properties);
            Map<Subscription, MessageListener> subscriptionTable = new HashMap<>();
            for (BaseConsumerHandler consumerHandler : baseConsumerHandlers){
                Subscription subscription = new Subscription();
                subscription.setTopic(consumerHandler.getTopic());
                subscription.setExpression(consumerHandler.getTag());
                subscription.setType(ExpressionType.TAG.name());
                subscriptionTable.put(subscription, (message, context) -> {
                    Action action = Action.CommitMessage;
                    try {
                        consumerHandler.doConsume(message,context);
                    }catch (Exception e){
                        action = Action.ReconsumeLater;
                    }
                    return action;
                });
            }
            consumerBean.setSubscriptionTable(subscriptionTable);
            consumerBeans.add(consumerBean);
        }
        return consumerBeans;
    }
}
