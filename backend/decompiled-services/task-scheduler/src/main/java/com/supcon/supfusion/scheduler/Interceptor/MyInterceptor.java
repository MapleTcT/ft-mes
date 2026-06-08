/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
 *  com.supcon.supfusion.scheduler.server.service.Utils.EscapeUtil
 *  org.apache.ibatis.executor.Executor
 *  org.apache.ibatis.mapping.BoundSql
 *  org.apache.ibatis.mapping.MappedStatement
 *  org.apache.ibatis.mapping.ParameterMapping
 *  org.apache.ibatis.plugin.Interceptor
 *  org.apache.ibatis.plugin.Intercepts
 *  org.apache.ibatis.plugin.Invocation
 *  org.apache.ibatis.plugin.Plugin
 *  org.apache.ibatis.plugin.Signature
 *  org.apache.ibatis.session.ResultHandler
 *  org.apache.ibatis.session.RowBounds
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.scheduler.Interceptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.scheduler.server.service.Utils.EscapeUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts(value={@Signature(type=Executor.class, method="query", args={MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class MyInterceptor
implements Interceptor {
    Logger LOGGER = LoggerFactory.getLogger(MyInterceptor.class);

    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement)args[0];
        Object parameterObject = args[1];
        BoundSql boundSql = statement.getBoundSql(parameterObject);
        String sql = boundSql.getSql();
        MyInterceptor.modifyLikeSql(sql, parameterObject, boundSql);
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        return Plugin.wrap((Object)target, (Interceptor)this);
    }

    public void setProperties(Properties properties) {
    }

    public static String modifyLikeSql(String sql, Object parameterObject, BoundSql boundSql) {
        if (!(parameterObject instanceof HashMap)) {
            return sql;
        }
        if (!sql.toLowerCase().contains(" like ") || !sql.toLowerCase().contains("?")) {
            return sql;
        }
        String[] strList = sql.split("\\?");
        HashSet<String> keyNames = new HashSet<String>();
        for (int i = 0; i < strList.length; ++i) {
            if (!strList[i].toLowerCase().contains(" like ")) continue;
            String keyName = ((ParameterMapping)boundSql.getParameterMappings().get(i)).getProperty();
            keyNames.add(keyName);
        }
        for (String keyName : keyNames) {
            Object a;
            HashMap parameter = (HashMap)parameterObject;
            if (keyName.contains("ew.paramNameValuePairs.") && sql.toLowerCase().contains(" like ?")) {
                String[] keyList;
                QueryWrapper wrapper = (QueryWrapper)parameter.get("ew");
                Object a2 = (parameter = (HashMap)wrapper.getParamNameValuePairs()).get((keyList = keyName.split("\\."))[2]);
                if (!(a2 instanceof String) || !a2.toString().contains("_") && !a2.toString().contains("\\") && !a2.toString().contains("%")) continue;
                parameter.put(keyList[2], "%" + EscapeUtil.escapeChar((String)a2.toString().substring(1, a2.toString().length() - 1)) + "%");
                continue;
            }
            if (!keyName.contains("ew.paramNameValuePairs.") && sql.toLowerCase().contains(" like ?")) {
                a = parameter.get(keyName);
                if (!(a instanceof String) || !a.toString().contains("_") && !a.toString().contains("\\") && !a.toString().contains("%")) continue;
                parameter.put(keyName, "%" + EscapeUtil.escapeChar((String)a.toString().substring(1, a.toString().length() - 1)) + "%");
                continue;
            }
            a = parameter.get(keyName);
            if (!(a instanceof String) || !a.toString().contains("_") && !a.toString().contains("\\") && !a.toString().contains("%")) continue;
            parameter.put(keyName, EscapeUtil.escapeChar((String)a.toString()));
        }
        return sql;
    }
}

