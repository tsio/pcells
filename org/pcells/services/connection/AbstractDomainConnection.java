package org.pcells.services.connection;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractDomainConnection implements DomainConnection
{
    protected List<DomainEventListener> _listener = new CopyOnWriteArrayList<DomainEventListener>();
    protected boolean _connected = false;

    protected synchronized void connected() {
        _connected = true;
        for (DomainEventListener listener : _listener) {
            try {
                listener.connectionOpened(this);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    protected synchronized void disconnected() {
        _connected = false;
        for (DomainEventListener listener : _listener) {
            try {
                listener.connectionClosed(this);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void addDomainEventListener(DomainEventListener listener) {
        _listener.add(listener);
        if (_connected) {
            try {
                listener.connectionOpened(this);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void removeDomainEventListener(DomainEventListener listener) {
        _listener.remove(listener);
    }

    public synchronized boolean isConnected() {
        return _connected;
    }
}
