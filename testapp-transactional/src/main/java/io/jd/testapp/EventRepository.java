package io.jd.testapp;

public interface EventRepository {
    Event findEvent(EventId eventId);

    void store(Event event);
}
