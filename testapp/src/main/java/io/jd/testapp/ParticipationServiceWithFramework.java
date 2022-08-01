package io.jd.testapp;

import jakarta.inject.Singleton;

import javax.transaction.Transactional;

@Singleton
public class ParticipationServiceWithFramework implements ParticipationService {
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;

    public ParticipationServiceWithFramework(ParticipantRepository participantRepository, EventRepository eventRepository) {
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    @Override
    public void participate(ParticipantId participantId, EventId eventId) {
        var participant = participantRepository.getParticipant(participantId);
        var event = eventRepository.findEvent(eventId);
        eventRepository.store(event.addParticipant(participant));
    }
}
