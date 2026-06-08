package com.supcon.supfusion.configuration.services.dao;

import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.framework.scaffold.hibernate.dao.impl.ExtGenDaoImpl;
import org.hibernate.HibernateException;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyDaoImpl extends ExtGenDaoImpl<Property, String> {

}
