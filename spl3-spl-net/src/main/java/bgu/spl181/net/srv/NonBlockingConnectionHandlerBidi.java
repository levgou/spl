package bgu.spl181.net.srv;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("Duplicates")
public class NonBlockingConnectionHandlerBidi<T> implements ConnectionHandler<T> {

    private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; //8k
    private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();
    public static int clientNum = 1;

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
    private final SocketChannel chan;
    private final Reactor reactor;
    public int id;
    // todo remove id - when remove prints

    public NonBlockingConnectionHandlerBidi(
            MessageEncoderDecoder<T> reader,
            BidiMessagingProtocol<T> protocol,
            SocketChannel chan,
            Reactor reactor) {

        this.chan = chan;
        this.encdec = reader;
        this.protocol = protocol;
        this.reactor = reactor;
    }

    @SuppressWarnings("Duplicates")
    public Runnable continueRead() {
        ByteBuffer buf = leaseBuffer();

        boolean success = false;
        try {
            success = chan.read(buf) != -1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (success) {
            Reactor.logger.debug(String.format("Client%d - read into buff successfully", id));
            buf.flip();
            return () -> {
                Reactor.logger.info("Start handle read for client" + id);
                try {
                    while (buf.hasRemaining()) {
                        T nextMessage = encdec.decodeNextByte(buf.get());

                        if (nextMessage != null) {
                            Reactor.logger.info(String.format("Client%d - Decoded Message: %s", id, nextMessage));
                            protocol.process(nextMessage);
                        }
                    }
                } finally {
                    releaseBuffer(buf);
                    Reactor.logger.info(String.format("Client%d - Release buffer", id));
                }
            };
        } else {
            Reactor.logger.info(String.format("Client%d - closing up", id));
            releaseBuffer(buf);
            close();
            return null;
        }

    }

    public void close() {
        try {
            chan.close();
            Reactor.logger.info(String.format("Client%d - closed chanel", id));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isClosed() {
        return !chan.isOpen();
    }

    public void continueWrite() {
        while (!writeQueue.isEmpty()) {
            try {
                ByteBuffer top = writeQueue.peek();
                chan.write(top);
                if (top.hasRemaining()) {
                    Reactor.logger.info(String.format("Written some for client%d but currently cant finish", id));
                    return;
                } else {
                    writeQueue.remove();
                    Reactor.logger.info(String.format("Finished writing msg for client%d return buff", id));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                close();
            }
        }

        if (writeQueue.isEmpty()) {
            if (protocol.shouldTerminate()) {
                Reactor.logger.warn("Should terminate client" + id);
                close();
            } else {
                reactor.updateInterestedOps(chan, SelectionKey.OP_READ);
                Reactor.logger.info(String.format("Finished writing all msgs for client%d put OP_READ only", id));
            }
        }
    }

    private static ByteBuffer leaseBuffer() {
        ByteBuffer buff = BUFFER_POOL.poll();
        if (buff == null) {
            Reactor.logger.debug("Gen new buffer");
            return ByteBuffer.allocateDirect(BUFFER_ALLOCATION_SIZE);
        }

        buff.clear();
        return buff;
    }

    private static void releaseBuffer(ByteBuffer buff) {
        BUFFER_POOL.add(buff);
    }

    /**
     * sync method to avoid race condition between broadcast and reply
     *
     * @param msg - msg to be sent
     */
    @Override
    synchronized public void send(T msg) {
        Reactor.logger.info(String.format("Client%d - Encoded Response: %s", id, msg));

        writeQueue.add(ByteBuffer.wrap(encdec.encode(msg)));
        reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        Reactor.logger.info(String.format("Updated Client%d with OP_RW", id));

    }
}
