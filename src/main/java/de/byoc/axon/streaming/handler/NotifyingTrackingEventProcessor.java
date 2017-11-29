package de.byoc.axon.streaming.handler;

import static java.util.Objects.requireNonNull;
import static org.axonframework.common.io.IOUtils.closeQuietly;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.axonframework.common.Assert;
import org.axonframework.common.AxonThreadFactory;
import org.axonframework.common.transaction.NoTransactionManager;
import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.AbstractEventProcessor;
import org.axonframework.eventhandling.ErrorHandler;
import org.axonframework.eventhandling.EventHandlerInvoker;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.UnableToClaimTokenException;
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore;
import org.axonframework.eventsourcing.eventstore.TrackingToken;
import org.axonframework.messaging.MessageStream;
import org.axonframework.messaging.StreamableMessageSource;
import org.axonframework.messaging.interceptors.TransactionManagingInterceptor;
import org.axonframework.messaging.unitofwork.RollbackConfiguration;
import org.axonframework.messaging.unitofwork.RollbackConfigurationType;
import org.axonframework.monitoring.MessageMonitor;
import org.axonframework.monitoring.NoOpMessageMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyingTrackingEventProcessor extends AbstractEventProcessor {

  private final static Logger logger = LoggerFactory.getLogger(NotifyingTrackingEventProcessor.class);

  private final StreamableMessageSource<TrackedEventMessage<?>> messageSource;
  private final TokenStore tokenStore;
  private final TransactionManager transactionManager;
  private final int batchSize;
  private final ExecutorService executorService;

  private volatile TrackingToken lastToken;
  private volatile State state = State.NOT_STARTED;
  private final Consumer<Object> onHead;

  public NotifyingTrackingEventProcessor(
          String name,
          EventHandlerInvoker eventHandlerInvoker,
          RollbackConfiguration rollbackConfiguration,
          ErrorHandler errorHandler,
          MessageMonitor<? super EventMessage<?>> messageMonitor, 
          
          StreamableMessageSource<TrackedEventMessage<?>> messageSource,
          TokenStore tokenStore,
          TransactionManager transactionManager, 
          int batchSize,
          Consumer<Object> onHead,
          ExecutorService executor) {
    super(name, eventHandlerInvoker, rollbackConfiguration, errorHandler, messageMonitor);

    this.messageSource = requireNonNull(messageSource);
    this.tokenStore = requireNonNull(tokenStore);
    this.transactionManager = transactionManager;
    this.executorService = executor;
    registerInterceptor(new TransactionManagingInterceptor<>(transactionManager));
    Assert.isTrue(batchSize > 0, () -> "batchSize needs to be greater than 0");
    this.batchSize = batchSize;
    this.onHead = onHead;
  }

  @Override
  public void start() {
    if (state == State.NOT_STARTED) {
      state = State.STARTED;
      registerInterceptor((unitOfWork, interceptorChain) -> {
        unitOfWork.onPrepareCommit(uow -> {
          EventMessage<?> event = uow.getMessage();
          if (event instanceof TrackedEventMessage<?>
                  && lastToken != null
                  && lastToken.equals(((TrackedEventMessage) event).trackingToken())) {
            tokenStore.storeToken(lastToken, getName(), 0);
          }
        });
        return interceptorChain.proceed();
      });
      executorService.submit(() -> {
        try {
          this.processingLoop();
        } catch (Throwable e) {
          logger.error("Processing loop ended due to uncaught exception. Processor stopping.", e);
        }
      });
    }
  }

  @Override
  public void shutDown() {
    if (state != State.SHUT_DOWN) {
      state = State.SHUT_DOWN;
      executorService.shutdown();
    }
  }

  /**
   * Fetch and process event batches continuously for as long as the processor is not shutting down.
   * The processor will process events in batches. The maximum size of size of each event batch is
   * configurable.
   * <p>
   * Events with the same tracking token (which is possible as result of upcasting) should always be
   * processed in the same batch. In those cases the batch size may be larger than the one
   * configured.
   */
  protected void processingLoop() {
    MessageStream<TrackedEventMessage<?>> eventStream = null;
    long errorWaitTime = 1;
    try {
      while (state != State.SHUT_DOWN) {
        eventStream = ensureEventStreamOpened(eventStream);
        try {
          processBatch(eventStream);
          errorWaitTime = 1;
        } catch (Exception e) {
          // make sure to start with a clean event stream. The exception may have cause an illegal state
          if (errorWaitTime == 1) {
            logger.warn("Error occurred. Starting retry mode.", e);
          }
          logger.warn("Releasing claim on token and preparing for retry in {}s", errorWaitTime);
          releaseToken();
          closeQuietly(eventStream);
          eventStream = null;
          try {
            Thread.sleep(errorWaitTime * 1000);
          } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            logger.warn("Thread interrupted. Preparing to shut down event processor");
            shutDown();
          }
          errorWaitTime = Math.min(errorWaitTime * 2, 60);
        }
      }
    } finally {
      closeQuietly(eventStream);
      releaseToken();
    }
  }

  private void releaseToken() {
    try {
      transactionManager.executeInTransaction(() -> tokenStore.releaseClaim(getName(), 0));
    } catch (Exception e) {
      // whatever.
    }
  }

  private void processBatch(MessageStream<TrackedEventMessage<?>> eventStream) throws Exception {
    List<TrackedEventMessage<?>> batch = new ArrayList<>();
    try {
      if (eventStream.hasNextAvailable(1, TimeUnit.SECONDS)) {
        while (batch.size() < batchSize && eventStream.hasNextAvailable()) {
          batch.add(eventStream.nextAvailable());
        }
     }
      if (batch.isEmpty()) {
        // refresh claim on token
        transactionManager.executeInTransaction(() -> tokenStore.extendClaim(getName(), 0));
        return;
      }

      // make sure all subsequent events with the same token (if non-null) as the last are added as well.
      // These are the result of upcasting and should always be processed in the same batch.
      lastToken = batch.get(batch.size() - 1).trackingToken();
      while (lastToken != null && eventStream.peek().filter(event -> lastToken.equals(event.trackingToken())).isPresent()) {
        batch.add(eventStream.nextAvailable());
      }

      process(batch);
      onHead.accept(lastToken);

    } catch (InterruptedException e) {
      logger.error(String.format("Event processor [%s] was interrupted. Shutting down.", getName()), e);
      Thread.currentThread().interrupt();
      this.shutDown();
    }
  }

  private MessageStream<TrackedEventMessage<?>> ensureEventStreamOpened(
          MessageStream<TrackedEventMessage<?>> eventStreamIn) {
    MessageStream<TrackedEventMessage<?>> eventStream = eventStreamIn;
    while (eventStream == null && state == State.STARTED) {
      Transaction tx = transactionManager.startTransaction();
      try {
        TrackingToken startToken = tokenStore.fetchToken(getName(), 0);
        eventStream = messageSource.openStream(startToken);
        tx.commit();
      } catch (UnableToClaimTokenException e) {
        tx.rollback();
        try {
          Thread.sleep(5000);
        } catch (InterruptedException interrupt) {
          logger.info("Thread interrupted while waiting for new attempt to claim token");
          Thread.currentThread().interrupt();
        }
      } catch (Exception e) {
        logger.warn("Unexpected exception while attemting to retrieve token and open stream. "
                + "Retrying in 5 seconds.", e);
        tx.rollback();
      }
    }
    return eventStream;
  }

  private enum State {
    NOT_STARTED, STARTED, SHUT_DOWN
  }
  
  public static class Builder {

    private final String name;
    private EventHandlerInvoker eventHandlerInvoker;
    private StreamableMessageSource<TrackedEventMessage<?>> messageSource;
    
    private final RollbackConfiguration rollbackConfiguration = RollbackConfigurationType.ANY_THROWABLE;
    private final ErrorHandler errorHandler = PropagatingErrorHandler.INSTANCE;
    private final TokenStore tokenStore = new InMemoryTokenStore();
    private final TransactionManager transactionManager = NoTransactionManager.INSTANCE;
    private int batchSize = 1;
    private final MessageMonitor<? super EventMessage<?>> messageMonitor = NoOpMessageMonitor.INSTANCE;
    
    private ExecutorService executor; 
    private Consumer<Object> onHead = x -> {};
    
    public Builder(String name) {
      this.name = name;
      executor = Executors.newSingleThreadExecutor(new AxonThreadFactory("TrackingEventProcessor - " + name));
    }
    
    public Builder withExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }
    
    public Builder withEventHandlerInvoker(EventHandlerInvoker invoker) {
      this.eventHandlerInvoker = invoker;
      return this;
    }
    
    public Builder withMessageSource(StreamableMessageSource<TrackedEventMessage<?>> messageSource) {
      this.messageSource = messageSource;
      return this;
    }
    
    public Builder onHead(Consumer<Object> consumer) {
      this.onHead = onHead.andThen(consumer);
      return this;
    }
    
    public Builder withBatchSize(int i) {
      this.batchSize = i;
      return this;
    }
    
    NotifyingTrackingEventProcessor build() {
      return new NotifyingTrackingEventProcessor(
              name, 
              eventHandlerInvoker, 
              rollbackConfiguration, 
              errorHandler,
              messageMonitor,
              messageSource, 
              tokenStore, 
              transactionManager, 
              batchSize, 
              onHead,
              executor);
    }
    
  }

}
