import bgu.spl181.net.impl.bidi.protocols.MovieMsgProtocol;
import bgu.spl181.net.dataholders.Movie;
import bgu.spl181.net.dataholders.UsersData;
import bgu.spl181.net.json.MovieDBInterface;
import bgu.spl181.net.srv.Server;
import bgu.spl181.net.impl.bidi.LineCmdEncoderDecoder;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class TestTpcParserServer {

    public static void main(String[] args) throws IOException {

//        testJson();
        serverMain();
    }

    private static void serverMain() {
        UsersData ud = new UsersData();

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


        Server<String> server = Server.<String>reactor(4,8686, genProtocols, genED);
        server.serve();
    }

    private static void testJson() throws IOException {
//        List<GenericUser> lenders = GenericUserDBInterface.getList();
//        System.out.println(lenders);
        List<Movie> movies = MovieDBInterface.getList();
        System.out.println(movies);
        movies.add(new Movie("80", "cyka", "80" ,"80","80", new String[]{}));
        MovieDBInterface.writeList(movies);
        movies = MovieDBInterface.getList();
        System.out.println(movies);
    }
}
