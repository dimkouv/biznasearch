package biznasearch.models;

public class Business {
    private String id;
    private String name;
    private double lat;
    private double lng;
    private String city;
    private int stars;
    private int reviewCount;
    private String address;
    private String postalCode;
    private String categories;
    private int clicks;

    public Business(String id, String name, double lat, double lng, String city, int stars, int reviewCount, String address, String postalCode, String categories, int clicks) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.city = city;
        this.stars = stars;
        this.reviewCount = reviewCount;
        this.address = address;
        this.postalCode = postalCode;
        this.categories = categories;
        this.clicks = clicks;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setClicks(int clicks) {
        this.clicks = clicks;
    }

    public int getClicks() {
        return clicks;
    }

    public String toString() {
        return this.name;
    }
}
