package de.byoc.axon.schema;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MySQL5Dialect;

public class JpaDump {

  public static void main(String... args) throws FileNotFoundException {

    try (PrintStream fos = new PrintStream(new File("target/axon-jpa.sql"))) {
      Configuration config = new Configuration();
      config.addAnnotatedClass(org.axonframework.eventsourcing.eventstore.jpa.DomainEventEntry.class);
      config.addAnnotatedClass(org.axonframework.eventsourcing.eventstore.jpa.SnapshotEventEntry.class);
      config.addAnnotatedClass(org.axonframework.eventhandling.saga.repository.jpa.AssociationValueEntry.class);
      config.addAnnotatedClass(org.axonframework.eventhandling.saga.repository.jpa.SagaEntry.class);
      config.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");
      config.setProperty(AvailableSettings.FORMAT_SQL, "true");
      
      String[] ddl = config.generateSchemaCreationScript(new MySQL5Dialect());

      for(String s : ddl) {
        fos.append(s);
        fos.append("\n\n");
      }

    }
  }

}
