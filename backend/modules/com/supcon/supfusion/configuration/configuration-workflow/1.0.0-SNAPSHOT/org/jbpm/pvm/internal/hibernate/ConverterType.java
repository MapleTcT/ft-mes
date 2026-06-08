/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.pvm.internal.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.AdaptedImmutableType;
import org.hibernate.type.EnumType;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.jbpm.api.JbpmException;
import org.jbpm.pvm.internal.type.Converter;
import org.jbpm.pvm.internal.util.ReflectUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Baeyens
 */
public class ConverterType extends AbstractSingleColumnStandardBasicType implements ParameterizedType {

  private static final long serialVersionUID = 1L;
  private Map<Class<?>, String> converterNames = null;
  private Map<String, Converter> converters = null;

  public ConverterType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor javaTypeDescriptor) {
    super(sqlTypeDescriptor, javaTypeDescriptor);
  }

//  @Override
//  public Object fromStringValue(String arg0) throws HibernateException {
//    return null;
//  }

  public Object get(ResultSet resultSet, String name) throws HibernateException, SQLException {
    String converterName = resultSet.getString(name);
    return converters.get(converterName);
  }

  public void set(PreparedStatement stmt, Object value, int index) throws HibernateException, SQLException {
    String converterName = (value!=null ? converterNames.get(value.getClass()) : null);
    stmt.setString(index, converterName);
  }

//  public int sqlType() {
//    return Types.VARCHAR;
//  }

//  @Override
//  public String toString(Object arg0) throws HibernateException {
//    return null;
//  }
//
//  @Override
//  public String getName() {
//    return "converter";
//  }


//  @Override
//  public boolean[] toColumnNullness(Object o, Mapping mapping) {
//    return new boolean[0];
//  }
//
//
//  @Override
//  public Size[] dictatedSizes(Mapping mapping) throws MappingException {
//    return new Size[0];
//  }
//
//  @Override
//  public Size[] defaultSizes(Mapping mapping) throws MappingException {
//    return new Size[0];
//  }


  @Override
  public void setParameterValues(Properties properties) {
    converterNames = new HashMap<Class<?>, String>();
    converters = new HashMap<String, Converter>();

    for(Object key : properties.keySet()) {
      String converterClassName = (String) key;
      try {
        Class< ? > converterClass = ReflectUtil.classForName(converterClassName);

        String converterName = properties.getProperty(converterClassName);
        converterNames.put(converterClass, converterName);
        Converter converter = (Converter) converterClass.newInstance();
        converters.put(converterName, converter);
      } catch (Exception e) {
        throw new JbpmException("couldn't initialize converter type "+converterClassName, e);
      }
    }
  }

  @Override
  public String getName() {
    return "converter";
  }
}
