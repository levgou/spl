package bgu.spl181.net.srv;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.impl.bidi.ConnectionsImpl;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;


@SuppressWarnings("Duplicates")
public class Reactor<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> readerFactory;
    private final ActorThreadPool pool;
    private final ConnectionsImpl<T> connections;
    private int currentConID;
    private Selector selector;

    private Thread selectorThread;
    private final ConcurrentLinkedQueue<Runnable> selectorTasks = new ConcurrentLinkedQueue<>();

    public static Logger logger = Logger.getLogger(Reactor.class);

    public Reactor(
            int numThreads,
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> readerFactory) {

        this.pool = new ActorThreadPool(numThreads);
        this.port = port;
        this.protocolFactory = protocolFactory;
        this.readerFactory = readerFactory;
        this.connections = new ConnectionsImpl<>();
        this.currentConID = 0;
    }

    @Override
    public void serve() {
        selectorThread = Thread.currentThread();
        try (Selector selector = Selector.open();
             ServerSocketChannel serverSock = ServerSocketChannel.open()) {

            this.selector = selector; //just to be able to close

            serverSock.bind(new InetSocketAddress(port));
            serverSock.configureBlocking(false);
            serverSock.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server started");
            logger.info("Server started");

            while (!Thread.currentThread().isInterrupted()) {

                selector.select();
                int keysNum = selector.selectedKeys().size();
                logger.info(String.format("Selector Woke Up for %d keys", keysNum));
                runSelectionThreadTasks();

                for (SelectionKey key : selector.selectedKeys()) {

                    if (!key.isValid()) {
                        continue;
                    } else if (key.isAcceptable()) {
                        logger.info("Accepting client" + NonBlockingConnectionHandlerBidi.clientNum);
                        handleAccept(serverSock, selector);
                        logger.info("Accepted client" + (NonBlockingConnectionHandlerBidi.clientNum - 1));
                    } else {
                        handleReadWrite(key);
                    }
                }

                selector.selectedKeys().clear(); //clear the selected keys set so that we can know about new events

            }

        } catch (ClosedSelectorException ex) {
            //do nothing - server was requested to be closed
        } catch (IOException ex) {
            //this is an error
            ex.printStackTrace();
        }

        System.out.println("server closed!!!");
        pool.shutdown();
    }

    /*package*/ void updateInterestedOps(SocketChannel chan, int ops) {
        final SelectionKey key = chan.keyFor(selector);

        if (Thread.currentThread() == selectorThread) {
            key.interestOps(ops);
        } else {
            selectorTasks.add(() -> {
                // sometimes broadcast happens before chanel was completely registered to selector
                if (key != null) {
                    key.interestOps(ops);
                }

            });
            selector.wakeup();
        }
    }


    private void handleAccept(ServerSocketChannel serverChan, Selector selector) throws IOException {
        SocketChannel clientChan = serverChan.accept();
        clientChan.configureBlocking(false);

        BidiMessagingProtocol<T> mp = protocolFactory.get();
        final NonBlockingConnectionHandlerBidi<T> handler = new NonBlockingConnectionHandlerBidi<>(
                readerFactory.get(),
                mp,
                clientChan,
                this);

        connections.addConnection(++currentConID, handler);
        mp.start(currentConID, connections);

        clientChan.register(selector, SelectionKey.OP_READ, handler);
        logger.info(String.format("Registered client%d with OP_READ", handler.id));
    }

    private void handleReadWrite(SelectionKey key) {
        // safe casting because its only thing we store at attachment
        NonBlockingConnectionHandlerBidi<T> handler = (NonBlockingConnectionHandlerBidi<T>) key.attachment();

        if (key.isReadable()) {
            logger.info("Handle read for client" + handler.id);
            Runnable task = handler.continueRead();
            if (task != null) {
                logger.info("Submit read task to pool for client" + handler.id);
                pool.submit(handler, task);
            }
        }

        if (key.isValid() && key.isWritable()) {
            logger.info("Trying to write for client" + handler.id);
            handler.continueWrite();
        }

    }

    private void runSelectionThreadTasks() {
        while (!selectorTasks.isEmpty()) {
            selectorTasks.remove().run();
        }
    }

    @Override
    public void close() throws IOException {
        selector.close();
    }

}
