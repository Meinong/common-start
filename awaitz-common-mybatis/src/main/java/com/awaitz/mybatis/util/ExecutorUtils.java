package com.awaitz.mybatis.util;

import com.awaitz.common.exception.AwaitzErrorException;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.mapping.BoundSql;

import java.lang.reflect.Field;
import java.util.Map;

public class ExecutorUtils {

    private static Field additionalParametersField;

    static {
        try {
            additionalParametersField = BoundSql.class.getDeclaredField("additionalParameters");
            additionalParametersField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new AwaitzErrorException("获取 BoundSql 属性 additionalParameters 失败: " + e);
        }
    }

    /**
     * 获取 BoundSql 属性值 additionalParameters
     *
     * @param boundSql
     * @return
     */
    public static Map<String, Object> getAdditionalParameter(BoundSql boundSql) {
        try {
            return (Map<String, Object>) additionalParametersField.get(boundSql);
        } catch (IllegalAccessException e) {
            throw new AwaitzErrorException("获取 BoundSql 属性值 additionalParameters 失败: " + e);
        }
    }
}
