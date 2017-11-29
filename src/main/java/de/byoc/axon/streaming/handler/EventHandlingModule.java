package de.byoc.axon.streaming.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.axonframework.config.Configuration;
import org.axonframework.config.ModuleConfiguration;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;
import org.axonframework.eventhandling.LoggingErrorHandler;
import org.axonframework.eventhandling.SimpleEventHandlerInvoker;

public class EventHandlingModule implements ModuleConfiguration {

  private List<EventHandlerBuilder> builders = new ArrayList<>();
  private NotifyingTrackingEventProcessor.Builder builder;

  private EventProcessor eventProcessor;

  public EventHandlingModule() {
    builder = new NotifyingTrackingEventProcessor.Builder("EventhandlingProcessor");
  }

  @Override
  public void initialize(Configuration config) {
    List<?> eh = builders.stream().map(b -> b.build(config)).collect(Collectors.toList());
    eventProcessor = builder
            .withEventHandlerInvoker(
                    new SimpleEventHandlerInvoker(eh,
                            config.parameterResolverFactory(),
                            config.getComponent(
                                    ListenerInvocationErrorHandler.class,
                                    LoggingErrorHandler::new)
                    ))
            .withExecutor(new ForkJoinPool())
            .withMessageSource(config.eventStore())
            .withBatchSize(10)
            .build();
  }

  @Override
  public void start() {
    eventProcessor.start();
  }

  @Override
  public void shutdown() {
    eventProcessor.shutDown();
  }

  EventHandlingModule registerEventHandler(EventHandlerBuilder fn) {
    builders.add(fn);
    return this;
  }

  EventHandlingModule onHead(Consumer<Object> onHead) {
    builder.onHead(onHead);
    return this;
  }

  EventHandlingModule batchSize(int i) {
    this.builder.withBatchSize(i);
    return this;
  }

  @FunctionalInterface
  interface EventHandlerBuilder {

    Object build(Configuration config);
  }

}
