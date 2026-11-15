package com.scaleup.simulation;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class SimulationSupport {

    private final SimulationSettings settings;
    private Random random;
    private long seenVersion;

    public SimulationSupport(SimulationSettings settings) {
        this.settings = settings;
        this.seenVersion = settings.version();
        this.random = new Random(settings.get().randomSeed());
    }

    // A number from 0.0 up to (not including) 1.0.
    // The sequence starts again whenever the settings are updated or reset.
    public synchronized double nextDouble() {
        long currentVersion = settings.version();
        if (currentVersion != seenVersion) {
            seenVersion = currentVersion;
            random = new Random(settings.get().randomSeed());
        }
        return random.nextDouble();
    }

    // Blocks the calling thread, like a real network call that waits for an answer.
    public void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulated call was interrupted", e);
        }
    }
}