package org.jbpm.pvm.internal.email.spi;

import javax.mail.Message;
import java.util.Collection;

/**
 * Pluggable control object for sending emails.
 * 
 * @author Brad Davis
 */
public interface MailSession {

  void send(Collection<Message> emails);

}
