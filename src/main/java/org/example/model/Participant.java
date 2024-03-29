package org.example.model;

public class Participant {
    private int score, id;
    private String country;

    public Participant(int id, int score, String country) {
        this.score = score;
        this.id = id;
        this.country = country;
    }
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getID() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String toString() {
        return this.id + " " + this.score + " " + this.country;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Participant)) {
            return false;
        }
        Participant participant = (Participant) o;
        return id == participant.id && score == participant.score && country.equals(participant.country);
    }
}
