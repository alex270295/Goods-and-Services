package korolyov.ctdev.itmo.services.items;

import java.util.Set;

public class Category {
    private String title;
    private Set<Integer> subs;

    public Category(String title, Set<Integer> subs) {
        this.title = title;
        this.subs = subs;
    }

    @Override
    public String toString() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public Set<Integer> getSubs() {
        return subs;
    }
}
