package io.jd.testapp;

import jakarta.inject.Singleton;

@Singleton
public class ParticipantRepositoryImpl implements ParticipantRepository {
    @Override
    public Participant getParticipant(ParticipantId participantId) {
        return new Participant();
    }
}
