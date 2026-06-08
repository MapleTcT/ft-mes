package org.jbpm.pvm.internal.lob;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.BlobProxy;
import org.jbpm.api.JbpmException;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Blob;
import java.sql.SQLException;

public class BlobStrategyBlob implements BlobStrategy {


  public void set(byte[] bytes, Lob lob) {
    if (bytes!=null) {
      lob.cachedBytes = bytes;
      Blob blob = BlobProxy.generateProxy(bytes);
      lob.blob = blob;
    }
  }

  public byte[] get(Lob lob) {
    if (lob.cachedBytes!=null) {
      return lob.cachedBytes;
    }
    
    java.sql.Blob sqlBlob = lob.blob;
    if (sqlBlob!=null) {
      try {
        return sqlBlob.getBytes(1, (int) sqlBlob.length());
      } catch (SQLException e) {
        throw new JbpmException("couldn't extract bytes out of blob", e);
      }
    } 
    return null;
  }
}
