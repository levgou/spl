package bgu.spl181.net.srv;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.impl.bidi.ConnectionsImpl;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private int currentConID;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;

    // TODO rethink connections field
    private ConnectionsImpl<T> connections;

    public static Logger logger = Logger.getLogger(BaseServer.class);


    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
        this.sock = null;
        this.connections = new ConnectionsImpl<>();
        this.currentConID = 0;
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
            System.out.println("Server started");
            logger.info("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();

                BidiMessagingProtocol<T> mp = protocolFactory.get();
                BlockingConnectionHandlerBidi<T> handler = new BlockingConnectionHandlerBidi<>(
                        clientSock,
                        encdecFactory.get(),
                        mp);

                // register to Connections & update MessageProtocol with ID & Connections
                // each time a connection is added - the client gets a new ID
                connections.addConnection(++currentConID, handler);
                mp.start(currentConID, connections);

                logger.info(String.format("Accepted Client%d", currentConID));

                execute(handler);
            }

        } catch (IOException ex) {
            logger.warn("Closing Server!");
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
        if (sock != null)
            sock.close();
    }

    protected abstract void execute(BlockingConnectionHandlerBidi<T> handler);

}
