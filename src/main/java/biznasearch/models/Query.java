package biznasearch.models;

public class Query {
    private long id;
    private String text;
    private int count;

    public Query(long id, String text, int count) {
        this.id = id;
        this.text = text;
        this.count = count;
    }

    public void setID(long id) {
        this.id = id;
    }

    public long getID() {
        return id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
