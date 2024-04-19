package org.sensorhub.impl.sensor.wearos.watch;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A value that resets to an initial value after a set time.
 *
 * @param <T> The type of the value.
 */
public class AutoResetValue<T> {
    private final T initialValue;
    private final int resetMillis;
    private final ScheduledExecutorService executorService;
    private T value;
    private long lastSetValueMillis;

    /**
     * Creates a new AutoResetValue with the given initial value and reset time.
     * When the value is set, it will be reset to the initial value after the reset time.
     *
     * @param initialValue The initial value.
     * @param resetMillis  The time in milliseconds to reset the value.
     */
    public AutoResetValue(T initialValue, int resetMillis) {
        this.initialValue = initialValue;
        this.resetMillis = resetMillis;
        this.value = initialValue;
        this.lastSetValueMillis = System.currentTimeMillis();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Returns the current value.
     *
     * @return The current value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets the value and starts the reset timer.
     * The value will be reset to the initial value after the reset time.
     *
     * @param value The new value.
     */
    public void setValue(T value) {
        this.value = value;
        this.lastSetValueMillis = System.currentTimeMillis();

        executorService.schedule(this::resetValue, resetMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Resets the value to the initial value.
     */
    private void resetValue() {
        if (System.currentTimeMillis() - lastSetValueMillis >= resetMillis) {
            value = initialValue;
        }
    }
}
