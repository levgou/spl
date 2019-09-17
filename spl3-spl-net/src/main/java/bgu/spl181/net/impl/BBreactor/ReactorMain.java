package bgu.spl181.net.impl.BBreactor;

import bgu.spl181.net.dataholders.UsersData;
import bgu.spl181.net.impl.bidi.LineCmdEncoderDecoder;
import bgu.spl181.net.impl.bidi.protocols.MovieMsgProtocol;
import bgu.spl181.net.json.MovieDBInterface;
import bgu.spl181.net.json.MovieLendersDBInterface;
import bgu.spl181.net.srv.Server;

import java.util.function.Supplier;

public class ReactorMain {
    public static void main(String[] args) {

        int pNum = Integer.parseInt(args[0]);

        UsersData ud = new UsersData();

        // reset buffers
        MovieLendersDBInterface.resetBuffer();
        MovieDBInterface.resetBuffer();

        Supplier genProtocols = new Supplier() {
            @Override
            public Object get() {
                return new MovieMsgProtocol(ud);
            }
        };

        Supplier genED = new Supplier() {
            @Override
            public Object get() {
                return new LineCmdEncoderDecoder();
            }
        };


        Server<String> server = Server.<String>reactor(4,pNum, genProtocols, genED);
        server.serve();

    }

}
