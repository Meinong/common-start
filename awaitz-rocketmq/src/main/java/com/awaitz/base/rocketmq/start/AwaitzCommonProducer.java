package com.awaitz.base.rocketmq.start;

import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.bean.ProducerBean;
import com.awaitz.base.rocketmq.config.MqConfig;
import com.awaitz.base.rocketmq.producer.AbstractAwaitzCommonProducer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

public class AwaitzCommonProducer extends AbstractAwaitzCommonProducer {

    @Autowired
    private MqConfig mqConfig;
    private final ProducerBean producerBean;


    public AwaitzCommonProducer(){
        producerBean = new ProducerBean();
    }


    @PostConstruct
    public synchronized void init(){
        Properties properties = mqConfig.getMqProperties();
        properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, "4000");
        producerBean.setProperties(mqConfig.getMqProperties());
        producerBean.start();
    }

    @PreDestroy
    public synchronized void destroy(){
        producerBean.shutdown();
    }


    @Override
    public Producer getProducer() {
        return this.producerBean;
    }
}
