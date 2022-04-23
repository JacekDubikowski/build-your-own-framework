package io.jd.testapp;

public class NoFrameworkApp {
    public static void main(String[] args) {
        ParticipationService participationService = new ManualTransactionParticipationService(
                new ParticipantRepositoryImpl(),
                new EventRepositoryImpl(),
                new TransactionalManagerStub()
        );
        participationService.participate(new ParticipantId(), new EventId());
    }
}
