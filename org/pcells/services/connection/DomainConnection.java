package org.pcells.services.connection;

import java.io.IOException;

public interface DomainConnection {
    String getAuthenticatedUser();
    int sendObject(Object obj,
                   DomainConnectionListener listener,
                   int id) throws IOException;
    int sendObject(String destination,
                   Object obj,
                   DomainConnectionListener listener,
                   int id) throws IOException;
    void addDomainEventListener(DomainEventListener listener);
    void removeDomainEventListener(DomainEventListener listener);
    void close() throws IOException;
}
