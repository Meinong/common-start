package com.awaitz.mybatis.autoconfigure;

import com.awaitz.mybatis.interceptor.EnvInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class AwaitzCommonMybatisAutoConfiguration {


    @Bean
    public EnvInterceptor envInterceptor(){
        return new EnvInterceptor();
    }

}
