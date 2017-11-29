
package de.byoc.axon.cqrs;


import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.AggregateConfigurer;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.EventHandlingConfiguration;
import org.axonframework.config.ModuleConfiguration;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;

public class AxonConfig {

  @Produces
  @ApplicationScoped
  public CommandGateway gateway(Configuration config) {
    return config.commandGateway();
  }
  
  @Produces
  @ApplicationScoped
  public ModuleConfiguration eventhandler() {
    EventHandlingConfiguration ehConfiguration = new EventHandlingConfiguration();
    ehConfiguration.registerEventHandler(c -> new NullHandler());
    ehConfiguration.registerHandlerInterceptor((c, s) -> (uwo, ic) -> {
      System.out
              .println(uwo.getMessage().getPayload() + " (" + uwo.getMessage().getTimestamp() + ")");
      return ic.proceed();
    });
    
    return ehConfiguration;
  }
  
  @Produces
  @ApplicationScoped
  public ModuleConfiguration aggregates() {
    final AggregateConfigurer<TestAggregate> aconf = 
            AggregateConfigurer.defaultConfiguration(TestAggregate.class);
    
    return aconf;
  }
  
  @Produces
  @ApplicationScoped
  public Configuration config(Instance<ModuleConfiguration> modules) {
    final Configurer config = DefaultConfigurer.defaultConfiguration();
    config.configureEmbeddedEventStore(c -> new InMemoryEventStorageEngine());
    
    modules.forEach(config::registerModule);
    
    final Configuration c = config.buildConfiguration();
    c.start();
    return c;
  }
  
}
