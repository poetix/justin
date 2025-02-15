package com.codepoetics.justin;

import java.util.concurrent.Semaphore;

public class SingleElementBlockingQueue<T> {

    private volatile T value;
    private final Semaphore readSemaphore = new Semaphore(0);
    private final Semaphore writeSemaphore = new Semaphore(1);

    public void write(T value) throws InterruptedException {
        writeSemaphore.acquire();
        this.value = value;
        readSemaphore.release();
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
