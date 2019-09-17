package bgu.spl181.net.dataholders;

import java.util.ArrayList;

public class MovieLender extends GenericUser {

    public String getType() {
        return type;
    }

    private final String type;
    private final String country;
    private String balance;
    private ArrayList<MovieListItem> movies;

    //todo add balance
    public MovieLender(String username, String pw, String type, String country, String balance) {
        super(username, pw);
        this.type = type;
        this.country = country;
        this.balance = balance;
        this.movies = new ArrayList<>();
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public boolean gotMovie(String movieName) {
        for (MovieListItem mli : movies) {
            if (mli.getName().equals(movieName)) {
                return true;
            }
        }

        return false;
    }

    public boolean fromOneOfCountries(String[] bannedCountries) {
        for (String bc : bannedCountries) {
            if (bc.equals(country)) {
                return true;
            }
        }

        return false;
    }

    public void updateRentage(Movie mv) {
        movies.add(new MovieListItem(mv.getId(), mv.getName()));
        balance = "" + (Integer.parseInt(balance) - Integer.parseInt(mv.getPrice()));
    }

    public void updateReturn(Movie mv) {
        movies.remove(new MovieListItem(mv.getId(), mv.getName()));
    }

    @Override
    public String toString() {
        return "MovieLender{" +
                "type='" + type + '\'' +
                ", country='" + country + '\'' +
                ", balance='" + balance + '\'' +
                ", movies=" + movies +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public ArrayList<MovieListItem> getMovies() {
        return movies;
    }

    public void setMovies(ArrayList<MovieListItem> movies) {
        this.movies = movies;
    }
}
