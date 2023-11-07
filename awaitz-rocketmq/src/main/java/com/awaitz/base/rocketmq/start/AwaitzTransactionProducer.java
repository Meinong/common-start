package com.awaitz.base.rocketmq.start;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.bean.TransactionProducerBean;
import com.awaitz.base.rocketmq.config.MqConfig;
import com.awaitz.base.rocketmq.producer.BaseTransactionProducerHandler;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

public class AwaitzTransactionProducer {

    @Autowired(required = false)
    private List<BaseTransactionProducerHandler> baseTransactionProducerHandlers;
    @Autowired
    private MqConfig mqConfig;

    private List<TransactionProducerBean> transactionProducerBeans;
    @PostConstruct
    public synchronized void init() throws Exception {
        setTransactionProducerBeans(buildTransactionProducerBeans());
        start();
    }

    @PreDestroy
    public synchronized void destroy(){
        shoudown();
    }


    public List<TransactionProducerBean> buildTransactionProducerBeans() throws Exception {
        List<TransactionProducerBean> transactionProducerBeans = new ArrayList<>();
        if(CollectionUtils.isEmpty(baseTransactionProducerHandlers)){
            return transactionProducerBeans;
        }
        Map<String,List<BaseTransactionProducerHandler>> groupIdAndHandlers =
                baseTransactionProducerHandlers.stream()
                        .collect(Collectors.groupingBy(BaseTransactionProducerHandler::getGroupId));

        for (Map.Entry<String,List<BaseTransactionProducerHandler>> entry : groupIdAndHandlers.entrySet()){
            //如果相同的groupId 有多个则报错
            if(entry.getValue().size() > NumberUtils.INTEGER_ONE){
                throw new Exception("当前groupId:[" + entry.getKey() + "]存在多个BaseTransactionProducerHandler实例");
            }
            BaseTransactionProducerHandler baseTransactionProducerHandler = entry.getValue().get(0);
            Properties properties = mqConfig.getMqProperties();
            properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, "4000");
            properties.setProperty(PropertyKeyConst.GROUP_ID,entry.getKey());
            baseTransactionProducerHandler.setProperties(properties);
            baseTransactionProducerHandler.setLocalTransactionChecker(baseTransactionProducerHandler.getLocalTransactionChecker());
            transactionProducerBeans.add(baseTransactionProducerHandler);
        }
        return transactionProducerBeans;
    }



    private void start(){
        for (TransactionProducerBean transactionProducerBean : transactionProducerBeans) {
            transactionProducerBean.start();
        }
    }

    private void shoudown(){
        for (TransactionProducerBean transactionProducerBean : transactionProducerBeans) {
            transactionProducerBean.shutdown();
        }
    }


    public void setTransactionProducerBeans(List<TransactionProducerBean> transactionProducerBeans) {
        this.transactionProducerBeans = transactionProducerBeans;
    }

}
