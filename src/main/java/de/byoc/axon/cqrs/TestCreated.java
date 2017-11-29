
package de.byoc.axon.cqrs;

public class TestCreated {

  private final String id;
  private final int someRandomInt;

  public TestCreated(String id, int someRandomInt) {
    this.id = id;
    this.someRandomInt = someRandomInt;
  }

  public String id() {
    return id;
  }
  
  @Override
  public String toString() {
    return String.format("id: %s, int: %s", id, someRandomInt);
  }
  
}
