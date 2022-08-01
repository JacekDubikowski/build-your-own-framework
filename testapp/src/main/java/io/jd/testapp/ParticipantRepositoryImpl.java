package io.jd.testapp;

public class ParticipantRepositoryImpl implements ParticipantRepository {
    @Override
    public Participant getParticipant(ParticipantId participantId) {
        return new Participant();
    }
}
