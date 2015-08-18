package korolyov.ctdev.itmo.services.tools;

import java.util.HashSet;
import java.util.Set;

public class Serializer {
    private static final String DIVIDER = "##";

    public static String serialize(Set<Integer> set) {
        if (set == null) {
            return null;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (Integer i : set) {
                stringBuilder.append(i);
                stringBuilder.append(DIVIDER);
            }
            return stringBuilder.toString();
        }
    }

    public static Set<Integer> deserialize(String str) {
        if (str == null) {
            return null;
        } else {
            String[] strings = str.split(DIVIDER);
            Set<Integer> set = new HashSet<>();
            for (String s : strings) {
                set.add(Integer.parseInt(s));
            }
            return set;
        }

    }

}
