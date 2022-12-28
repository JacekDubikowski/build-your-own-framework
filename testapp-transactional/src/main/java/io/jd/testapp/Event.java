package io.jd.testapp;

public record Event() {
    Event addParticipant(Participant participant) {
        return new Event();
    }
}
