package biznasearch.models;

public class Tip {
    private long id;
    private String text;
    private String date;
    private int complimentCount;
    private String businessId;

    public Tip(long id, String text, String date, int complimentCount, String businessId) {
        this.id = id;
        this.text = text;
        this.date = date;
        this.complimentCount = complimentCount;
        this.businessId = businessId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getComplimentCount() {
        return complimentCount;
    }

    public void setComplimentCount(int complimentCount) {
        this.complimentCount = complimentCount;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }
}
