package bgu.spl181.net.json;

import bgu.spl181.net.dataholders.MovieLender;
import bgu.spl181.net.srv.BaseServer;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class MovieLendersDBInterface {

    private static Logger logger = BaseServer.logger;

    // create buffer for when only a read is needed
    private static List<MovieLender> buffer;
    public static final String filePath = "Database/Users.json";
    private static final String listName = "users";
    private static final Type t = new TypeToken<Map<String, List<MovieLender>>>() {
    }.getType();

    public static List<MovieLender> getList() {
        return JsonDBInterface.getListFromBuffer(buffer, listName, filePath, t);
    }

    public static void writeList(List<MovieLender> userList) {
        try {
            buffer = userList;
            JsonDBInterface.<MovieLender>writeList(listName, userList, filePath);
        } catch (IOException e) {
            return;
        }

        // todo rethink exception
    }

    public static void addUser(String name, String pw, String country) {
        List<MovieLender> l = getList();
        l.add(new MovieLender(name, pw, "normal",
                StringUtils.substringsBetween(country, "=\"", "\"")[0], "0"));
        writeList(l);
    }

    public static boolean userExists(String username) {
        for (MovieLender ml : getList()) {
            if (ml.getUsername().equals(username)) {
                return true;
            }
        }

        return false;
    }


    public static boolean userPWexists(String username, String pw) {
        for (MovieLender ml : getList()) {
            if (ml.getUsername().equals(username) && ml.getPassword().equals(pw)) {
                return true;
            }
        }

        return false;
    }

    public static boolean userIsAdmin(String userName) {
        for (MovieLender ml : getList()) {
            if (ml.getUsername().equals(userName)) {
                return ml.getType().equals("admin");
            }
        }// for

        // user not found:
        return false;
    }

    public static MovieLender getUser(String userName) {
        for (MovieLender ml : getList()) {
            if (ml.getUsername().equals(userName)) {
                return ml;
            }
        }// for
        return null;
    }

    public static String addShmeckels(String userName, int amount) {
        String fin = "meh";
        List<MovieLender> l = getList();
        for (MovieLender ml : l) {
            if (ml.getUsername().equals(userName)) {
                logger.info(String.format("User: %s old balance %s", userName, ml.getBalance()));
                ml.setBalance("" + (Integer.parseInt(ml.getBalance()) + amount));
                logger.info(String.format("User: %s new balance %s", userName, ml.getBalance()));
                fin = ml.getBalance();
                break;
            }
        }// for

        if (!fin.equals("meh")) {
            writeList(l);
//            logger.info(l);
        }

        return fin;
    }

    public static void writeMovieLender(MovieLender movieLender) {
        List<MovieLender> l = getList();
        for (MovieLender ml : l) {
            if (movieLender.getUsername().equals(ml.getUsername())) {
                ml.setBalance(movieLender.getBalance());
                ml.setMovies(movieLender.getMovies());
            }
        }
        writeList(l);
    }

    public static void resetBuffer() {
        buffer = null;
    }
}
