package org.example.model;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class ConcurrentLinkedList {

    private Node head;
    private Node tail;
    private int size;
    private Set<Integer> blackList = new ConcurrentSkipListSet<>();

    public ConcurrentLinkedList() {
        this.head = new Node(null);
        this.tail = new Node(null);
        this.size = 0;
        head.setNext(tail);
    }

    public Node getHead() {
        return head;
    }

    public void setHead(Node head) {
        this.head = head;
    }




    public void insert(Participant participant) {
        Node newNode = new Node(participant);
        Node current = head;
        current.lock();
        Node next = current.getNext();
        next.lock();
        current.setNext(newNode);
        newNode.setNext(next);
        current.unlock();
        next.unlock();
    }


    public void delete(int id) {
        Node prev = head;
        prev.lock();
        Node current = prev.getNext();
        current.lock();
        try {
            while (current != tail) {
                if (current.getParticipant().getID() == id) {
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

   public Integer get(int id) {
    Node current = head;
    current.lock();
    try {
        while (current != tail) {
            Participant participant = current.getParticipant();
            if (participant != null && participant.getID() == id) {
                return current.getParticipant().getScore();
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

    public void addParticipant(Participant participant) {
        if (get(participant.getID()) != null) {
            if (participant.getScore() == -1) {
                delete(participant.getID());
                blackList.add(participant.getID());
            } else {
                update(participant.getID(), participant.getScore());
            }
        } else {
            if (participant.getScore() != -1 && !blackList.contains(participant.getID())) {
                insert(participant);
            } else if (participant.getScore() == -1) {
                blackList.add(participant.getID());
            }
        }
    }

    public void update(Integer id, Integer score) {

        Node current = head;
        current.lock();
        try {
            while (current != tail) {
                Participant participant = current.getParticipant();
                if (participant != null && participant.getID() == id) {
                    participant.setScore(participant.getScore() + score);
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
    class ParticipantComparator implements Comparator<Participant> {
        @Override
        public int compare(Participant o1, Participant o2) {
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            if (o1.getScore() == o2.getScore()) {
                return o1.getID() - o2.getID();
            }
            return o2.getScore() - o1.getScore();
        }
    }

    public void sort() {
        List<Node> nodes = getAll();
        System.out.println(nodes.size());
        Collections.sort(nodes, Comparator.comparing(Node::getParticipant, new ParticipantComparator()));

        Node newHead = new Node(null);
        Node current = newHead;

        for (Node node : nodes) {
            current.setNext(node);
            current = node;
        }

        current.setNext(tail);

        setHead(newHead);
    }

}