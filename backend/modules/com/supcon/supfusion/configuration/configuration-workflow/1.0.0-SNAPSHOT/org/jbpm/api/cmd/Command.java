package org.jbpm.api.cmd;

import org.jbpm.api.ProcessEngine;

import java.io.Serializable;

/** commands that can be {@link ProcessEngine#execute(Command) executed by the process engine}.
 * 
 * @author Tom Baeyens
 */
public interface Command<T> extends Serializable {
  
  T execute(Environment environment) throws Exception;
}
