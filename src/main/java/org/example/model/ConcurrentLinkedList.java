package org.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConcurrentLinkedList {

    private Node head;
    private Node tail;
    private int size;

    public ConcurrentLinkedList() {
        this.head = new Node(null, null);
        this.tail = new Node(null, null);
        this.size = 0;
        head.setNext(tail);
    }

    public Node getHead() {
        return head;
    }

    public void setHead(Node head) {
        this.head = head;
    }




    public void insert(String key, Integer value) {
        Node newNode = new Node(key, value);
        Node current = head;
        current.lock();
        Node next = current.getNext();
        next.lock();
        current.setNext(newNode);
        newNode.setNext(next);
        current.unlock();
        next.unlock();
    }


    public void delete(String key) {
        Node prev = head;
        prev.lock();
        Node current = prev.getNext();
        current.lock();
        try {
            while (current != tail) {
                if (current.getKey().equals(key)) {
                    Node next = current.getNext();
                    next.lock();
                    prev.setNext(next);
                    next.unlock();

                    return;
                }
                prev.unlock();
                prev = current;
                prev.lock();
                Node next = current.getNext();
                next.lock();
                current.unlock();
                current = next;
                next.unlock();
                current.lock();
            }
        } finally {
            prev.unlock();
            current.unlock();
        }
    }

   public Integer get(String key) {
    Node current = head;
    current.lock();
    try {
        while (current != tail) {
            String currentKey = current.getKey();
            if (currentKey != null && currentKey.equals(key)) {
                return current.getValue();
            }
            Node next = current.getNext();
            next.lock();
            current.unlock();
            current = next;
            current.lock();
            next.unlock();
        }
    }
    finally {
        current.unlock();
    }

    return null;
}

    public List<Node> getAll() {
        List<Node> nodes = new ArrayList<>();
        Node current = getHead();
        current.lock();
        try{
            while (current != tail) {
                nodes.add(current) ;
                Node next = current.getNext();
                next.lock();
                current.unlock();
                current = next;
                next.unlock();
                current.lock();

            }
        }
        finally{
            current.unlock();
        }

        return nodes;
    }

    public void update(String key, Integer value) {

        Node current = head;
        current.lock();
        try {
            while (current != tail) {
                String currentKey = current.getKey();
                if (currentKey != null && currentKey.equals(key)) {
                    current.setValue(current.getValue() + value);
                    return;
                }
                Node next = current.getNext();
                next.lock();
                current.unlock();
                current = next;
                next.unlock();
                current.lock();
            }
        } finally {
            current.unlock();
        }
    }

    public void sort() {
        List<Node> nodes = getAll();

        Collections.sort(nodes, Comparator.comparing(Node::getValue, Comparator.nullsLast(Comparator.reverseOrder())));

        Node newHead = new Node(null, null);
        Node current = newHead;

        for (Node node : nodes) {
            current.setNext(node);
            current = node;
        }

        current.setNext(tail);

        setHead(newHead);
    }

}