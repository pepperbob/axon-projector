
package de.byoc.eventing.events;

public class InvoicePaid {

  public final Integer amount;
  
  public InvoicePaid(Integer amount) {
    this.amount = amount;
  }
  
}
