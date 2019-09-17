package bgu.spl181.net.dataholders;

import java.util.Objects;

public class Movie {

    private String id;
    private final String name;
    private String price;
    private String availableAmount;
    private String totalAmount; // todo maybe final
    private String[] bannedCountries; // todo maybe final

    public Movie(String id, String name, String price, String availableAmount, String totalAmount, String[] bannedCountries) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.availableAmount = availableAmount;
        this.totalAmount = totalAmount;
        this.bannedCountries = bannedCountries;
    }

    public String getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(String availableAmount) {
        this.availableAmount = availableAmount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public String[] getBannedCountries() {
        return bannedCountries;
    }

    public void decreaseAvailability() {
        availableAmount = "" + (Integer.parseInt(availableAmount) - 1);
    }

    public void increaseAvailability() {
        availableAmount = "" + (Integer.parseInt(availableAmount) + 1);
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(id, movie.id) &&
                Objects.equals(name, movie.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
