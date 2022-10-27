package io.jd.testapp;

import jakarta.inject.Singleton;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;


@Singleton
public class ManualTransactionParticipationService implements ParticipationService {
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final TransactionManager transactionManager;

    public ManualTransactionParticipationService(ParticipantRepository participantRepository, EventRepository eventRepository, TransactionManager transactionManager) {
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public void participate(ParticipantId participantId, EventId eventId) {
        try {
            transactionManager.begin();
            var participant = participantRepository.getParticipant(participantId);
            var event = eventRepository.findEvent(eventId);
            eventRepository.store(event.addParticipant(participant));

            System.out.printf("Participant: '%s' takes part in event: '%s'%n", participant, event);

            transactionManager.commit();
        } catch (Exception e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    private void rollback() {
        try {
            transactionManager.rollback();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}
