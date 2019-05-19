package biznasearch.models;

public class SearchResult {
    Business business;
    String highlight;

    public SearchResult(Business business, String highlight) {
        this.business = business;
        this.highlight = highlight;
    }

    public SearchResult(String highlight) {
        this.highlight = highlight;
    }

    public Business getBusiness() {
        return business;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }
}
