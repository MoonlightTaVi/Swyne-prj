package utils;

public class PassedTime {
    private long lastCheck;
    private final String name;
    public PassedTime(String name) {
        lastCheck = System.currentTimeMillis();
        this.name = name;
        System.out.printf("Time count for \"%s\" started.%n", name);
    }
    public void makeStamp(String note) {
        long newCheck = System.currentTimeMillis();
        System.out.printf("Time passed (%s) at \"%s\": %dms.%n", note, name, (newCheck - lastCheck));
        lastCheck = newCheck;
    }
}
