package io.jd.testapp;

import jakarta.inject.Singleton;

@Singleton
public class EventRepositoryImpl implements EventRepository {

    @Override
    public void store(Event event) {
    }

    @Override
    public Event findEvent(EventId eventId) {
        return new Event();
    }
}
