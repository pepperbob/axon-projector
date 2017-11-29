
package de.byoc.axon.cqrs;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.commandhandling.model.AggregateLifecycle;
import org.axonframework.commandhandling.model.AggregateRoot;
import org.axonframework.eventsourcing.EventSourcingHandler;

@AggregateRoot
public class TestAggregate {

  @AggregateIdentifier
  private String id;
  
  public TestAggregate() {}
  
  @CommandHandler
  public TestAggregate(CreateTest cmd) {
    AggregateLifecycle.apply(new TestCreated(cmd.id(), ((int)(Math.random()*100))));
  }
  
  @EventSourcingHandler
  public void on(TestCreated test) {
    this.id = test.id();
  }
  
}
