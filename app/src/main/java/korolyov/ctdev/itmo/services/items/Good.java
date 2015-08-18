package korolyov.ctdev.itmo.services.items;

import java.util.Set;

public class Good {
    private int id;
    private String title;
    private Set<Integer> subs;

    public Good(int id, String title, Set<Integer> subs) {
        this.id = id;
        this.title = title;

        this.subs = subs;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Set<Integer> getSubs() {
        return subs;
    }

    @Override
    public String toString() {
        return title;
    }
}
