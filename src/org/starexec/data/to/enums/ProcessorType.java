package org.starexec.data.to.enums;

/**
 * Represents the type of the processor (along with it's SQL storage values)
 */
public enum ProcessorType {
    DEFAULT(0),
    PRE(1),
    POST(2),
    BENCH(3),
    UPDATE(4);

    private int val;

    ProcessorType(int val) {
        this.val = val;
    }

    public int getVal() {
        return this.val;
    }

    public static ProcessorType valueOf(int val) {
        switch(val) {
            case 1:
                return PRE;
            case 2:
                return POST;
            case 3:
                return BENCH;
            case 4:
                return UPDATE;
            default:
                return DEFAULT;
        }
    }
}
