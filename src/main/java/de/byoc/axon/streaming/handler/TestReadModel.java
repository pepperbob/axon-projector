package de.byoc.axon.streaming.handler;

import de.byoc.axon.streaming.events.TableSeated;
import de.byoc.eventing.events.FoodOrdered;
import de.byoc.eventing.events.InvoicePaid;
import org.axonframework.eventhandling.EventHandler;

public class TestReadModel {

  public int numberItemsOrdered;
  public int totalQuantity;
  public int invoiceTotal;
  public int avgInvoiceItem;
  public int totalPersons;
  public int salesPerPerson;

  @EventHandler
  public void falls(FoodOrdered event) {
    this.numberItemsOrdered++;
    this.totalQuantity += event.qty;
  }

  @EventHandler
  public void falls(InvoicePaid event) {
    this.invoiceTotal = event.amount;
    this.avgInvoiceItem = this.invoiceTotal / this.totalQuantity;
  }
  
  @EventHandler
  public void falls(TableSeated event) {
    this.totalPersons += event.persons;
    this.salesPerPerson = totalPersons == 0 ? 0 : invoiceTotal/totalPersons;
  }
  
  @EventHandler
  private void abc(TableSeated event) {
    this.totalPersons += event.persons;
    this.salesPerPerson = totalPersons == 0 ? 0 : invoiceTotal/totalPersons;
  }

}
