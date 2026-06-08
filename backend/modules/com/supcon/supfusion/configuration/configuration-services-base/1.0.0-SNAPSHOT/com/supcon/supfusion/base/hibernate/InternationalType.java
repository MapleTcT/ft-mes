package com.supcon.supfusion.base.hibernate;

import com.supcon.supfusion.base.i18n.InternationalBaseResource;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;


public class InternationalType implements UserType,ParameterizedType {
	private Properties parameters;
	private String language;

	@Override
	public int[] sqlTypes() {
		return new int[]{Types.VARCHAR,Types.VARCHAR};
	}

	@Override
	public Class returnedClass() {
		return String.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return ( x == y ) || ( x != null && x.equals( y ) );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		String result = rs.getString( names[0] );
		if ( rs.wasNull() ) {
			return null;
		}
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, sqlTypes()[0] );
			st.setNull( index+1, sqlTypes()[1] );
		}
		else {
			String string = (String) value;
			st.setString( index, string );
			if(null == language) {
				language = InternationalBaseResource.getDefaultLanguage();
			}
			String text = InternationalBaseResource.get(string,language);
			if(null == text) {
				st.setNull(index + 1, sqlTypes()[1]);
			} else {
				st.setString( index+1, text);
			}

		}
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		if (value==null) {
			return null;
		}
		return (Serializable)value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner)
			throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner)
			throws HibernateException {
		return original;
	}

	@Override
	public void setParameterValues(Properties parameters) {
		this.parameters=parameters;
		if(parameters!=null){
			language=parameters.getProperty("language");
		}
	}

}

