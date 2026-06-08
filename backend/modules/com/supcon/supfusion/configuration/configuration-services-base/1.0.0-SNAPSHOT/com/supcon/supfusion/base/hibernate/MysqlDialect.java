package com.supcon.supfusion.base.hibernate;

import org.hibernate.dialect.MySQL55Dialect;

import java.sql.Types;

public class MysqlDialect extends MySQL55Dialect {
    public MysqlDialect() {
        super();
        registerHibernateType(Types.NULL, "string");
    }
}
