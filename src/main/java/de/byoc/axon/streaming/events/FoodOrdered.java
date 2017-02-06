
package de.byoc.eventing.events;

public class FoodOrdered {

  public String menuitem;
  public int qty;
  
  public FoodOrdered(String menuitem, int qty) {
    this.menuitem = menuitem;
    this.qty = qty;
  }
  
}
