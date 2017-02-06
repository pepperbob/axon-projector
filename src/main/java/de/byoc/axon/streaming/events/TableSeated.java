package de.byoc.axon.streaming.events;

public class TableSeated {

  public final String table;
  public final Integer persons;
  
  public TableSeated(String table, Integer persons) {
    this.table = table;
    this.persons = persons;
  }
  
}
