package org.starexec.logger;

public class NonSavingStarLogger extends BaseStarLogger {

    private NonSavingStarLogger(Class clazz) {
        super(clazz);
    }
    private NonSavingStarLogger(String name) {
        super(name);
    }

    public static NonSavingStarLogger getLogger(Class clazz) {
        return new NonSavingStarLogger(clazz);
    }
    public static NonSavingStarLogger getLogger(String name) {
        return new NonSavingStarLogger(name);
    }
    @Override
    protected void log(StarLevel level, final String method, final String message, final Throwable t) {
        final String prefixedMessage = method == null ? message : prefix(method)+message;

        if (t == null) {
            log.log(level.get(), prefixedMessage);
        } else {
            log.log(level.get(), prefixedMessage, t);
        }
    }
}
