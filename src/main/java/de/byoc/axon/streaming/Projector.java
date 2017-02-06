package de.byoc.axon.streaming;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageStream;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.StreamableMessageSource;

public final class Projector {

  private final StreamableMessageSource<? extends Message<?>> source;

  public Projector(StreamableMessageSource<? extends Message<?>> source) {
    this.source = source;
  }

  public void project(Object handler) {
    final ProjectionMethods methods = new ProjectionMethods(handler.getClass().getMethods());

    try (final MessageStream<? extends Message<?>> stream = this.source.openStream(null)) {
      while (stream.hasNextAvailable()) {
        final Message<?> next = stream.nextAvailable();

        methods.filter(next.getPayloadType())
                .forEach(method -> method.invoke(handler, next));
      }
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  private class ProjectionMethods {

    private Map<Class<?>, MethodInvoker> map = new HashMap<>();

    public ProjectionMethods(Method[] methods) {
      for (Method method : methods) {
        final boolean meta;
        if (method.getParameterCount() == 1) {
          meta = false;
        } else if (method.getParameterCount() == 2
                && method.getParameterTypes()[1].isAssignableFrom(MetaData.class)) {
          meta = true;
        } else {
          continue;
        }
        
        map.put(method.getParameterTypes()[0], new MethodInvoker(method, meta));
      }
    }

    public Stream<MethodInvoker> filter(Class<?> type) {
      return map.keySet().stream()
              .filter(clazz -> clazz.isAssignableFrom(type))
              .map(map::get);
    }
  }

  private class MethodInvoker {

    private final Method method;
    private final boolean meta;

    private MethodInvoker(Method method, boolean meta) {
      this.method = method;
      this.meta = meta;
    }

    public void invoke(Object target, Message<?> message) {
      try {
        if (meta) {
          method.invoke(target, message.getPayload(), message.getMetaData());
        } else {
          method.invoke(target, message.getPayload());
        }
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
        throw new RuntimeException(ex.getCause() != null ? ex.getCause() : ex);
      }
    }
  }

}
