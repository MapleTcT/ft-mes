package com.supcon.supfusion.base.hibernate;

import com.supcon.supfusion.base.utils.Inflector;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;


/**
 * An improved naming strategy that prefers embedded underscores to mixed case names
 *
 * @see DefaultNamingStrategy the default strategy
 * @author Gavin King
 */
public class BAPNamingStrategy extends SpringPhysicalNamingStrategy{

    /**
     *
     */
    private static final long serialVersionUID = 8296829403131050579L;
    /**
     * A convenient singleton instance
     */
    public static final SpringPhysicalNamingStrategy INSTANCE = new BAPNamingStrategy();

    public static final String[] entities = new String[] { "Module", "ModuleRelation", "Entity", "Model", "Property", "View", "DataGroup", "DataClassific",
            "Sql", "BusinessCenter", "DataGrid", "ExtraView", "FastQueryJson", "AdvQueryJson","AdvQueryCondition","BackupView", "ExtraQueryJson", "Field", "Button", "Event",
            "Validate", "BackupDataGrid","CustomerCondition","ModuleReference", "PrintTemplate","ImportTemplate","SchedulerJob", "Echarts", "EchartsModel"};

    private String tableNamePrefix = "Ec";

    public void setTableNamePrefix(String tableNamePrefix) {
        this.tableNamePrefix = tableNamePrefix;
    }
    //重写classToTableName方法，传入model名称，例如：Property；和tableNamePrefix进行拼接，生成例如"Runtime_Property"的表名称 by cwn 20190525
    @Override
    public Identifier toPhysicalTableName(Identifier name,
                                          JdbcEnvironment jdbcEnvironment) {
        String entitiesPath =name.getText();
        for (String tmp : entities) {
            if (entitiesPath.equals(tmp)) {
                return new Identifier((tableNamePrefix+"_"+ Inflector.underscore(entitiesPath)).toLowerCase(), false);
            }
        }
        return  new Identifier((Inflector.underscore(entitiesPath)),false);
    }

}
