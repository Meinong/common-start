package com.awaitz.base.rocketmq.config;


import com.awaitz.base.rocketmq.start.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MqConfig.class)
public class EnableAutoMqConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public AwaitzCommonConsumer consumerClient(){
        return new AwaitzCommonConsumer();
    }

    @Bean
    @ConditionalOnMissingBean
    public AwaitzOrderConsumer orderConsumerBean(){
        return new AwaitzOrderConsumer();
    }

    @Bean
    @ConditionalOnMissingBean
    public AwaitzCommonProducer producer(){
        return new AwaitzCommonProducer();
    }

    @Bean
    @ConditionalOnMissingBean
    public AwaitzOrderProducer orderProducer(){
        return new AwaitzOrderProducer();
    }

    @Bean
    @ConditionalOnMissingBean
    public AwaitzTransactionProducer transactionProducer(){
        return new AwaitzTransactionProducer();
    }

}
