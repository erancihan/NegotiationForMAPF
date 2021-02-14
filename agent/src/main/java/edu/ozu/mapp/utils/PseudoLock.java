package edu.ozu.mapp.utils;

import java.util.concurrent.atomic.AtomicReference;

public class PseudoLock
{
    private final AtomicReference<Boolean> lock = new AtomicReference<>(Boolean.FALSE);

    /**
     * @return {@code true}  - lock is false and will be locked.<br>
     *         {@code false} - already locked.
     * */
    public boolean tryLock() {
        return lock.compareAndSet(Boolean.FALSE, Boolean.TRUE);
    }

    /**
     * @return {@code true}  - lock is true and will be unlocked.<br>
     *         {@code false} - already unlocked.
     * */
    public boolean unlock() {
        return lock.compareAndSet(Boolean.TRUE, Boolean.FALSE);
    }
}
