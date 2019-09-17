package bgu.spl181.net.json;

import bgu.spl181.net.dataholders.Movie;
import bgu.spl181.net.dataholders.MovieLender;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MovieDBInterface {

    // create buffer for when only a read is needed
    private static List<Movie> buffer;
    private static final String listName = "movies";
    private static final String filePath = "Database/Movies.json";
    private static final Type t = new TypeToken<Map<String, List<Movie>>>() {
    }.getType();

    public static List<Movie> getList() {
        return JsonDBInterface.getListFromBuffer(buffer, listName, filePath, t);
    }

    public static void writeList(List<Movie> movieList) {
        try {
            buffer = movieList;
            JsonDBInterface.<Movie>writeList("movies", movieList, filePath);
        } catch (IOException e) {
            return;
        }
        // todo rethink exception handle
    }

    public static Movie getMovie(String movieName) {
        for (Movie mv : getList()) {
            if (mv.getName().equals(movieName)) {
                return mv;
            }
        }
        return null;
    }

    public static void writeMovie(Movie movie) {
        List<Movie> l = getList();
        for (Movie m : l) {
            if (movie.getName().equals(m.getName())) {
                m.setAvailableAmount(movie.getAvailableAmount());
                m.setPrice(movie.getPrice());
            }
        }
        writeList(l);
    }

    public static boolean addMovie(Movie movie) {
        Integer highestId = 0;
        List<Movie> l = getList();
        for (Movie m : l) {
            if (movie.getName().equals(m.getName())) {
                return false;
            }
            highestId = Math.max(highestId, Integer.parseInt(m.getId()));
        }

        // at this place movie is not in db:
        movie.setId("" + (highestId + 1));
        l.add(movie);
        writeList(l);
        return true;
    }

    public static int removeMovie(String movieName) {
        Movie suspect = null;
        List<Movie> l = getList();
        for (Movie m : l) {
            if (movieName.equals(m.getName())) {
                suspect = m;
                break;
            }
        }

        if (suspect == null) {
            return 1;
        } else if (!suspect.getAvailableAmount().equals(suspect.getTotalAmount())) {
            return 2;
        }
        // can remove
        else {
            l.remove(suspect);
        }

        writeList(l);
        return 0;
    }

    public static String changePrice(String movieName, String newPrice) {
        Movie suspect = null;
        List<Movie> l = getList();
        for (Movie m : l) {
            if (movieName.equals(m.getName())) {
                suspect = m;
                break;
            }
        }

        if (suspect == null) {
            return "meh";
        }

        // found movie:
        suspect.setPrice(newPrice);
        writeList(l);
        return suspect.getAvailableAmount();
    }

    public static void resetBuffer() {
        buffer = null;
    }
}
