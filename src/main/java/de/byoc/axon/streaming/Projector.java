package de.byoc.axon.streaming;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.SimpleEventHandlerInvoker;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageHandler;
import org.axonframework.messaging.MessageStream;
import org.axonframework.messaging.StreamableMessageSource;

public final class Projector {

  private final StreamableMessageSource<? extends Message<?>> source;
  
  public Projector(StreamableMessageSource<? extends Message<?>> source) {
    this.source = source;
  }

  public void project(Object handler) {
    final MessageHandler<EventMessage<?>> invoker = new SimpleEventHandlerInvoker(handler);
    try (final MessageStream<? extends Message<?>> stream = this.source.openStream(null)) {
      while (stream.hasNextAvailable()) {
        final Message<?> next = stream.nextAvailable();
        if (next instanceof EventMessage) {
          invoker.handle((EventMessage<?>) next);
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}