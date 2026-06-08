package org.jbpm.pvm.internal.email.impl;

import javax.mail.Authenticator;
import javax.mail.Session;
import java.util.Properties;

/**
 * Settings for establishing a session with a mail server.
 * 
 * @author Brad Davis
 */
public class MailServer {

  private AddressFilter addressFilter;
  private Properties sessionProperties;
  private Authenticator authenticator;
  private volatile Session mailSession;

  public AddressFilter getAddressFilter() {
    return addressFilter;
  }

  protected void setAddressFilter(AddressFilter filter) {
    this.addressFilter = filter;
  }

  public Properties getSessionProperties() {
    return sessionProperties;
  }

  protected void setSessionProperties(Properties sessionProperties) {
    this.sessionProperties = sessionProperties;
  }

  public Authenticator getAuthenticator() {
    return authenticator;
  }

  protected void setAuthenticator(Authenticator authenticator) {
    this.authenticator = authenticator;
  }

  public Session getMailSession() {
    if (mailSession == null) {//多线程错误 - 可能对属性进行了双重检测 
      synchronized (this) {
        if (mailSession == null) {
          mailSession = Session.getInstance(sessionProperties, authenticator);
        }
      }
    }
    return mailSession;
  }

  protected void setMailSession(Session mailSession) {
    this.mailSession = mailSession;
  }
}
