package com.awaitz.base.rocketmq.start;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.bean.OrderConsumerBean;
import com.aliyun.openservices.ons.api.bean.Subscription;
import com.aliyun.openservices.ons.api.order.MessageOrderListener;
import com.aliyun.openservices.ons.api.order.OrderAction;
import com.awaitz.base.rocketmq.config.MqConfig;
import com.awaitz.base.rocketmq.consumer.BaseOrderConsumerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

public class AwaitzOrderConsumer {

    @Autowired
    private MqConfig mqConfig;
    @Autowired(required = false)
    private List<BaseOrderConsumerHandler> baseOrderConsumerHandlerList;

    private List<OrderConsumerBean> orderConsumerBeans;



    @PostConstruct
    public synchronized void init(){
        List<OrderConsumerBean> orderConsumerBeans= buildConsumerStartInfo();
        this.setConsumerBeans(orderConsumerBeans);
        start();
    }

    @PreDestroy
    public synchronized void destroy(){
        shutdown();
    }


    private void start(){
        for (OrderConsumerBean consumerBean : orderConsumerBeans){
            consumerBean.start();
        }
    }

    private void shutdown(){
        for (OrderConsumerBean consumerBean : orderConsumerBeans){
            consumerBean.shutdown();
        }
    }

    private void setConsumerBeans(List<OrderConsumerBean> orderConsumerBeans) {
        this.orderConsumerBeans = orderConsumerBeans;
    }



    private List<OrderConsumerBean> buildConsumerStartInfo() {

        //将相同的GroupId 组合成一个OrderConsumerBean
        List<OrderConsumerBean> consumerBeans = new ArrayList<>();
        if(CollectionUtils.isEmpty(baseOrderConsumerHandlerList)){
            return consumerBeans;
        }
        //相同的GroupId 组合在一起
        Map<String,List<BaseOrderConsumerHandler>> handlerMap =
                baseOrderConsumerHandlerList.stream().collect(Collectors.groupingBy(BaseOrderConsumerHandler::getGroupId));


        for (Map.Entry<String,List<BaseOrderConsumerHandler>> mapList: handlerMap.entrySet()){
            String groupId = mapList.getKey();
            List<BaseOrderConsumerHandler> baseConsumerHandlers = mapList.getValue();
            OrderConsumerBean orderConsumerBean = new OrderConsumerBean();
            Properties properties = mqConfig.getMqProperties();
            properties.setProperty(PropertyKeyConst.GROUP_ID, groupId);
            orderConsumerBean.setProperties(properties);
            Map<Subscription, MessageOrderListener> subscriptionTable = new HashMap<>();
            for (BaseOrderConsumerHandler consumerHandler : baseConsumerHandlers){
                Subscription subscription = new Subscription();
                subscription.setTopic(consumerHandler.getTopic());
                subscription.setExpression(consumerHandler.getTag());
                subscriptionTable.put(subscription, (message, context) -> {
                    OrderAction action = OrderAction.Success;
                    try {
                        consumerHandler.doConsume(message,context);
                    }catch (Exception e){
                        action = OrderAction.Suspend;
                    }
                    return action;
                });
            }
            orderConsumerBean.setSubscriptionTable(subscriptionTable);
            consumerBeans.add(orderConsumerBean);
        }
        return consumerBeans;
    }
}
