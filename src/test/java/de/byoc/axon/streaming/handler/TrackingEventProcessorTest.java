package de.byoc.axon.streaming.handler;

import java.util.concurrent.atomic.AtomicLong;

import de.byoc.eventing.events.FoodOrdered;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TrackingEventProcessorTest {

  private Configuration c;
  private TestReadModel myTestor;
  private AtomicLong count = new AtomicLong();
  
  @Before
  public void before() {
    this.myTestor = new TestReadModel();
    
    final Configurer config = DefaultConfigurer.defaultConfiguration();
    config.configureEmbeddedEventStore(c -> new InMemoryEventStorageEngine());
    config.registerModule(new EventHandlingModule()
            .registerEventHandler(c -> myTestor)
            .onHead(x -> count.incrementAndGet())
            .batchSize(3));
    
    c = config.buildConfiguration();
    c.start();
  }
  
  @After
  public void after() {
    c.shutdown();
  }
  
  @Test
  public void test() throws InterruptedException {
    c.eventBus().publish(GenericEventMessage.asEventMessage(new FoodOrdered("Something", 3)));
    c.eventBus().publish(GenericEventMessage.asEventMessage(new FoodOrdered("Something", 2)));
    c.eventBus().publish(GenericEventMessage.asEventMessage(new FoodOrdered("Something", 3)));
    
    Thread.sleep(1500L);
    
    Assert.assertEquals(8, myTestor.totalQuantity);
    Assert.assertEquals(1L, count.get());
  }
  
}
