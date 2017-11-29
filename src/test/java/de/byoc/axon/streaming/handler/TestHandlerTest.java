package de.byoc.axon.streaming.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.byoc.axon.streaming.Projector;
import de.byoc.axon.streaming.events.TableSeated;
import de.byoc.eventing.events.FoodOrdered;
import de.byoc.eventing.events.InvoicePaid;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.axonframework.messaging.StreamableMessageSource;
import org.junit.Before;
import org.junit.Test;

public class TestHandlerTest {

  Projector projector;

  TestReadModel readmodel;

  @Before
  public void setup() {
    projector = new Projector(
            given(new TableSeated("table-1", 3),
                    new FoodOrdered("Item 1", 2),
                    new TableSeated("table-2", 10),
                    new FoodOrdered("Item 2", 3),
                    new InvoicePaid(120)));

    readmodel = new TestReadModel();
    projector.project(readmodel);
  }

  @Test
  public void testSomeMethod() {
    assertThat(readmodel.numberItemsOrdered, is(2));
  }

  @Test
  public void avgInvoiceItem() {
    assertThat(readmodel.avgInvoiceItem, is(24));
  }

  @Test
  public void invoiceTotal() {
    assertThat(readmodel.invoiceTotal, is(120));
  }

  @Test
  public void numberItemsOrdered() {
    assertThat(readmodel.numberItemsOrdered, is(2));
  }

  @Test
  public void totalQuantity() {
    assertThat(readmodel.totalQuantity, is(5));
  }

  private StreamableMessageSource<?> given(Object... events) {
    final InMemoryEventStorageEngine engine = new InMemoryEventStorageEngine();
    EmbeddedEventStore es = new EmbeddedEventStore(engine);
    es.publish(
            Stream.of(events)
                    .map(payload -> new GenericEventMessage<>(payload))
                    .collect(Collectors.toList()));
    
    return es;
  }

}
