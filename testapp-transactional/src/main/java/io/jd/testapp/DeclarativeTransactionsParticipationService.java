package io.jd.testapp;

import jakarta.inject.Singleton;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

@Singleton
public class DeclarativeTransactionsParticipationService implements ParticipationService {
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;

    public DeclarativeTransactionsParticipationService(ParticipantRepository participantRepository, EventRepository eventRepository) {
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    public void participate(ParticipantId participantId, EventId eventId) {
            var participant = participantRepository.getParticipant(participantId);
            var event = eventRepository.findEvent(eventId);
            eventRepository.store(event.addParticipant(participant));

            System.out.printf("Participant: '%s' takes part in event: '%s'%n", participant, event);
    }
}
