package bgu.spl181.net.impl.bidi;

import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.srv.BaseServer;
import bgu.spl181.net.srv.ConnectionHandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {


    private Map<Integer, ConnectionHandler<T> > connections;
    private static Logger logger = BaseServer.logger;

    public ConnectionsImpl() {
        connections = new ConcurrentHashMap<>();
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> ch = connections.get(connectionId);
        if (ch == null) {
            logger.warn(String.format("Didn't find ConnectionHandler for Client%d can't reply!", connectionId));
            return false;
        }

        // TODO: 1/3/18 maybe this could except somehow - try/catch with return false should do
        ch.send(msg);
        return true;

    }

    /**
     * iter over ch and send msg, base assumption - connections map includes only live connections
     *
     * @param msg - message to send
     */
    @Override
    public void broadcast(T msg) {

        for (Integer key : connections.keySet()) {
            send(key, msg);
        }
    }

    /**
     * remove from map & get back the ch -> close the ch
     *
     * @param connectionId -  connection to be d/c
     */
    @Override
    public void disconnect(int connectionId) {

        ConnectionHandler ch = connections.remove(connectionId);

        // close the ch
        try {
            ch.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        removeConnection(connectionId);

    }

    public void addConnection(Integer id, ConnectionHandler<T> ch) {
        connections.put(id, ch);
        logger.info("Added ConnectionHandler for Client" + id);
    }

    protected void removeConnection(Integer id) {
        connections.remove(id);
    }
}
