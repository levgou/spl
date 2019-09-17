package bgu.spl181.net.srv;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandlerBidi<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private static Logger logger = BaseServer.logger;

    public BlockingConnectionHandlerBidi(Socket sock,
                                         MessageEncoderDecoder<T> reader,
                                         BidiMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    // TODO: 1/3/18 remove bellow annotation
    @SuppressWarnings("Duplicates")
    @Override
     public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;


                in = new BufferedInputStream(sock.getInputStream());
                out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                }
            }

            logger.warn("Conection finished with: ");

        } catch (IOException ex) {
            logger.fatal(ExceptionUtils.getStackTrace(ex));
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    /**
     * sync method to avoid race condition between broadcast and reply
     *
     * @param msg - msg to be sent
     */
    @Override
    synchronized public void send(T msg) {
        try {

            // sometimes someone else's broadcast outruns creation of out
            if (out == null) {
                return;
            }

            out.write(encdec.encode(msg));
            out.flush();
            logger.info(String.format("Sent <%s>", msg));
        } catch (IOException ex) {
            logger.fatal(ExceptionUtils.getStackTrace(ex));
        }

    }
}
