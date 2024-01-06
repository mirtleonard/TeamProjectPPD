package org.example.model;

import java.util.concurrent.locks.ReentrantLock;

public class Node {
    private String key;
    private Integer value;
    private Node next;
    private final ReentrantLock lock = new ReentrantLock();


    public Node(String key, Integer value) {
        this.key = key;
        this.value = value;
        this.next = null;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
    public String getKey() {
        return key;
    }


    public void setKey(String key) {
        this.key = key;
    }


    public Integer getValue() {
        return value;
    }


    public void setValue(Integer value) {
        this.value = value;
    }


    public Node getNext() {
        lock.lock();
        try {
            return next;
        } finally {
            lock.unlock();
        }
    }

    public void setNext(Node next) {
        lock.lock();
        try {
            this.next = next;
        } finally {
            lock.unlock();
        }
    }

}
