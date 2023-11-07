package com.awaitz.base.rocketmq.start;

import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.bean.OrderProducerBean;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.awaitz.base.rocketmq.config.MqConfig;
import com.awaitz.base.rocketmq.producer.AbstractAwaitzCommonProducer;
import com.awaitz.base.rocketmq.producer.AbstractAwaitzOrderProducer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

public class AwaitzOrderProducer extends AbstractAwaitzOrderProducer {

    @Autowired
    private MqConfig mqConfig;
    private final OrderProducerBean orderProducerBean;


    public AwaitzOrderProducer(){
        orderProducerBean = new OrderProducerBean();
    }


    @PostConstruct
    public synchronized void init(){
        Properties properties = mqConfig.getMqProperties();
        properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, "4000");
        orderProducerBean.setProperties(mqConfig.getMqProperties());
        orderProducerBean.start();
    }

    @PreDestroy
    public synchronized void destroy(){
        orderProducerBean.shutdown();
    }

    @Override
    public OrderProducer getOrderProducer() {
        return orderProducerBean;
    }
}
