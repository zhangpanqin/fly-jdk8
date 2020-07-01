package java.lang.management;

/**
 * Types of {@link MemoryPoolMXBean memory pools}.
 */
public enum MemoryType {


    HEAP("Heap memory"),


    NON_HEAP("Non-heap memory");

    private final String description;

    private MemoryType(String s) {
        this.description = s;
    }

    @Override
    public String toString() {
        return description;
    }

    private static final long serialVersionUID = 6992337162326171013L;
}
