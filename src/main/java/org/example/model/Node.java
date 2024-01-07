package org.example.model;

import java.util.concurrent.locks.ReentrantLock;

public class Node {
    private Participant participant;
    private Node next;
    private final ReentrantLock lock = new ReentrantLock();


    public Node(Participant participant) {
        this.participant = participant;
        this.next = null;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public int getScore() {
        Participant participant = this.getParticipant();
        if (participant == null) {
            return -1;
        }
        return participant.getScore();
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
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
