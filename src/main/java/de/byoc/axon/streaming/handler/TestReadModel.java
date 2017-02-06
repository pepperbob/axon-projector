package de.byoc.axon.streaming.handler;

import de.byoc.axon.streaming.events.TableSeated;
import de.byoc.eventing.events.FoodOrdered;
import de.byoc.eventing.events.InvoicePaid;

public class TestReadModel {

  public int numberItemsOrdered;
  public int totalQuantity;
  public int invoiceTotal;
  public int avgInvoiceItem;
  public int totalPersons;
  public int salesPerPerson;

  public void falls(FoodOrdered event) {
    this.numberItemsOrdered++;
    this.totalQuantity += event.qty;
  }

  public void falls(InvoicePaid event) {
    this.invoiceTotal = event.amount;
    this.avgInvoiceItem = this.invoiceTotal / this.totalQuantity;
  }
  
  public void falls(TableSeated event) {
    this.totalPersons += event.persons;
    this.salesPerPerson = totalPersons == 0 ? 0 : invoiceTotal/totalPersons;
  }
  
  private void abc(TableSeated event) {
    this.totalPersons += event.persons;
    this.salesPerPerson = totalPersons == 0 ? 0 : invoiceTotal/totalPersons;
  }

}
