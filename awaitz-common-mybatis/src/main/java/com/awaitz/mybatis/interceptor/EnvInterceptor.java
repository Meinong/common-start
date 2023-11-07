package com.awaitz.mybatis.interceptor;

import com.awaitz.mybatis.util.ExecutorUtils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NamedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Intercepts(
        {
                @Signature(type = Executor.class, method = "update",
                        args = {MappedStatement.class, Object.class}),
                @Signature(type = Executor.class, method = "query",
                        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query",
                        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                                CacheKey.class, BoundSql.class})
        }
)
public class EnvInterceptor implements Interceptor {

    @Value("${spring.profiles.active:}")
    private String env;

    @Value("${select.envs:}")
    private List<String> selectEnvs;

    private static final String ENV_COLUMN = "env";
    private static final String SQL_STR = "sql";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        Executor executor = (Executor) invocation.getTarget();
        MappedStatement ms = (MappedStatement) args[0];
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        if (SqlCommandType.SELECT.equals(sqlCommandType)) {
            return execQuery(invocation,ms,executor,args);
        }else if (SqlCommandType.INSERT.equals(sqlCommandType)){
            return execInsert(ms,executor,args);
        }
        return executor.update(ms,args[1]);
    }

    private Object execInsert(MappedStatement ms,Executor executor,Object[] args) throws Throwable {
        Object parameter = args[1];
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();
        Statement stmt = CCJSqlParserUtil.parse(sql);
        Insert insertstmt = (Insert) stmt;
        intoValue(insertstmt);
        //通过反射修改sql语句
        Field field = boundSql.getClass().getDeclaredField(SQL_STR);
        field.setAccessible(true);
        field.set(boundSql, insertstmt.toString());
        MappedStatement newStatement = newMappedStatement(ms, new EnvSqlSource(boundSql));
        args[0] = newStatement;
        return executor.update(newStatement,parameter);
    }

    private Object execQuery(Invocation invocation,MappedStatement ms,Executor executor,Object[] args) throws Throwable {
        Object parameter = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        ResultHandler resultHandler = (ResultHandler) args[3];
        CacheKey cacheKey;
        BoundSql boundSql;
        //由于逻辑关系，只会进入一次
        if (invocation.getArgs().length == 4) {
            //4 个参数时
            boundSql = ms.getBoundSql(parameter);
            cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
        } else {
            //6 个参数时
            boundSql = (BoundSql) args[5];
            cacheKey = (CacheKey) args[4];
        }
        String sql = boundSql.getSql();
        String newSql;
        Statement stmt; //解析sql
        stmt = CCJSqlParserUtil.parse(sql);
        Select select = (Select) stmt;
        SelectBody selectBody = select.getSelectBody();
        if(selectBody instanceof WithItem){
            //with 查询不拦截
            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
        } else if(selectBody instanceof PlainSelect){
            PlainSelect plainSelect = (PlainSelect) selectBody;
            if(plainSelect.getFromItem() instanceof SubSelect){
                //子查询不拦截
                return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
            }
            //非子查询时构建新的sql
            newSql = buildselectNewSql(plainSelect);
            //构建新的boundSql
            BoundSql newBoundSql = buildNewBoundSql(boundSql, newSql, parameter, ms);
            //execut query
            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, newBoundSql);
        }
        //其他情况不拦截
        return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
    }



    private BoundSql buildNewBoundSql(BoundSql boundSql,String newSql,Object parameter,MappedStatement ms){
        Map<String, Object> additionalParameters = ExecutorUtils.getAdditionalParameter(boundSql);
        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), newSql, boundSql.getParameterMappings(), parameter);
        for (String key : additionalParameters.keySet()) {
            newBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
        }
        return newBoundSql;
    }

    //构建环境变量
    private String envStr(){
        //环境变量
        String selectEnvStr;
        if(CollectionUtils.isEmpty(selectEnvs)){
            selectEnvStr = env;
        }else{
            selectEnvStr = String.join("','",selectEnvs);
        }
        return selectEnvStr;
    }

    //构建新的sql
    private String buildselectNewSql(PlainSelect plainSelect) throws JSQLParserException {
        StringBuilder whereStr = new StringBuilder();
        //是否是join查询
        if(plainSelect.getJoins() != null && plainSelect.getJoins().size() > 0){
            whereStr.append(plainSelect.getFromItem().getAlias().getName()).append(".env in ('").append(envStr()).append("')").append(" and ");
            List<Join> joins = plainSelect.getJoins();
            for (Join join : joins) {
                //左查询不拼接
                if (join.getRightItem() != null && !join.isLeft()) {
                    String alias = join.getRightItem().getAlias().getName();
                    whereStr.append(alias).append(".env in ('").append(envStr()).append("')").append(" and ");
                }
            }
        }else{
            whereStr.append("env in ('").append(envStr()).append("')");
        }
        Expression condExpression = CCJSqlParserUtil.parseCondExpression(whereStr.toString());
        plainSelect.setWhere(plainSelect.getWhere() != null ? new AndExpression(plainSelect.getWhere(),condExpression) : condExpression);
        return plainSelect.toString();
    }


    /**
     * insert sql add column
     * @param insert
     */
    private void intoValue(Insert insert) {
        // 添加列
        insert.getColumns().add(new Column(ENV_COLUMN));
        // 通过visitor设置对应的值
        insert.getItemsList(ExpressionList.class).accept(new ItemsListVisitor() {
            @Override
            public void visit(ExpressionList expressionList) {
                List<Expression> expressions;
                if(expressionList.isUsingBrackets()){
                    expressions = expressionList.getExpressions();
                    setExpressions(env,expressions);
                }else{
                    for (Expression expression : expressionList.getExpressions()) {
                        RowConstructor rowConstructor = (RowConstructor)expression;
                        expressions = rowConstructor.getExprList().getExpressions();
                        setExpressions(env,expressions);
                    }
                }
            }
            @Override
            public void visit(MultiExpressionList multiExpressionList) {
                List<Expression> expressions;
                for (ExpressionList expressionList : multiExpressionList.getExpressionLists()) {
                    expressions = expressionList.getExpressions();
                    setExpressions(env,expressions);
                }
            }
            @Override
            public void visit(SubSelect subSelect) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void visit(NamedExpressionList namedExpressionList) {
                throw new UnsupportedOperationException("Not supported yet.");
            }


        });
    }

    private void setExpressions(final Object columnValue,List<Expression> expressions){
        if (columnValue instanceof String) {
            expressions.add(new StringValue((String) columnValue));
        } else if (columnValue instanceof Long) {
            expressions.add(new LongValue((Long) columnValue));
        } else {
            // if you need to add other type data, add more if branch
            expressions.add(new StringValue((String) columnValue));
        }
    }


    private static MappedStatement newMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder =
                new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (String keyProperty : ms.getKeyProperties()) {
                keyProperties.append(keyProperty).append(",");
            }
            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
            builder.keyProperty(keyProperties.toString());
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

    static class EnvSqlSource implements SqlSource {

        private final BoundSql boundSql;

        public EnvSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }
}
