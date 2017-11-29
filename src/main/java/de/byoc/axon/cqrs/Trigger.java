
package de.byoc.axon.cqrs;

import java.util.UUID;

import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.axonframework.commandhandling.gateway.CommandGateway;

@Stateless
public class Trigger {

  @Inject
  private CommandGateway gateway;
  
  @Schedules({
    @Schedule(hour = "*", minute = "*", second = "*", info = "Trigger Timer *"),
    @Schedule(hour = "*", minute = "*", second = "*", info = "Trigger Timer */3"),
    @Schedule(hour = "*", minute = "*", second = "*", info = "Trigger Timer */7"),
    @Schedule(hour = "*", minute = "*", second = "*", info = "Trigger Timer */11"),
    @Schedule(hour = "*", minute = "*", second = "*", info = "Trigger Timer */13"),
    @Schedule(hour = "*", minute = "*", second = "*", info = "Trigger Timer */17")
  })
  public void trigger1() {
    gateway.send(new CreateTest(UUID.randomUUID().toString()));
  }
  
}
