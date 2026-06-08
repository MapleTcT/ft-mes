package com.supcon.supfusion.base.hibernate;

import org.hibernate.dialect.MariaDB53Dialect;

import java.sql.Types;

public class MariadbDialect extends MariaDB53Dialect {
    public MariadbDialect() {
        super();
        registerHibernateType(Types.NULL, "string");
    }

}
