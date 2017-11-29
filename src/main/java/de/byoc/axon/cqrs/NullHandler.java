
package de.byoc.axon.cqrs;

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;

public class NullHandler {

  @EventHandler
  public void on(EventMessage<?> msg) {
    
  }
  
}
