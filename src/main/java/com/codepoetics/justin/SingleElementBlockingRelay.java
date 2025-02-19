package com.codepoetics.justin;

import java.util.concurrent.Semaphore;

public class SingleElementBlockingRelay<T> {

    private volatile T value;
    private final Semaphore readSemaphore = new Semaphore(0);
    private final Semaphore writeSemaphore = new Semaphore(0);

    public void write(T value) throws InterruptedException {
        this.value = value;
        readSemaphore.release();
        writeSemaphore.acquire();
    }

    public T read() throws InterruptedException {
        try {
            readSemaphore.acquire();
            return this.value;
        } finally {
            this.value = null;
            writeSemaphore.release();
        }
    }

    public void drain() {
        if (readSemaphore.tryAcquire()) {
            this.value = null;
            writeSemaphore.release();
        }
    }
}
