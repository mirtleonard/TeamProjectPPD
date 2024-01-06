package org.example.model;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CustomQueue {
    LinkedList<Participant> queue = new LinkedList<>();
    private final ReentrantLock LOCK = new ReentrantLock();
    private final Condition NOT_EMPTY = LOCK.newCondition();
    private final Condition NOT_FULL = LOCK.newCondition();
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(true);

    private static Integer maxCapacity;

    public CustomQueue(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public void setFinished() {
        IS_RUNNING.set(false);
        LOCK.lock();
        NOT_EMPTY.signal();
        LOCK.unlock();
    }

    public void add(Participant participant) {
        LOCK.lock();
        try {
            while (queue.size() == maxCapacity) {
                NOT_FULL.await();
            }
            queue.add(participant);
            NOT_EMPTY.signal(); //notify the consumers
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            LOCK.unlock();
        }
    }

    public synchronized Participant get() {
        Participant participant = null;
        LOCK.lock();
        try {
            while (queue.size() == 0 && IS_RUNNING.get()) {
                NOT_EMPTY.await();
            }
            participant = queue.pollFirst();
            NOT_FULL.signal(); //notify the producers
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        } finally {
            LOCK.unlock();
        }
        return participant;
    }
}
