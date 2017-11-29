package de.byoc.axon.schema;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.axonframework.eventhandling.saga.repository.jdbc.GenericSagaSqlSchema;
import org.axonframework.eventsourcing.eventstore.jdbc.EventSchema;
import org.axonframework.eventsourcing.eventstore.jdbc.MySqlEventTableFactory;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SchemaDump {

  public static void main(String... args) throws SQLException, FileNotFoundException {
    try (PrintStream fos = new PrintStream(new File("target/axon.sql"))) {
      Connection connection = Mockito.mock(Connection.class);
      fos.append("\n\n--\n\n");
      Mockito.when(connection.prepareStatement(Mockito.anyString())).thenAnswer(new Answer() {
        @Override
        public Object answer(InvocationOnMock iom) throws Throwable {
          String xy = iom.getArgument(0);
          System.out.println(xy);
          fos.append(xy);
          fos.append("\n\n");
          return null;
        }
      });

      EventSchema schema = new EventSchema();
      final MySqlEventTableFactory instance = MySqlEventTableFactory.INSTANCE;
      instance.createDomainEventTable(connection, schema);
      instance.createSnapshotEventTable(connection, schema);
      
      new GenericSagaSqlSchema().sql_createTableAssocValueEntry(connection);
      new GenericSagaSqlSchema().sql_createTableSagaEntry(connection);
      
    }
  }

}
