package io.jd.testapp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ParticipationDTO(ParticipantId participantId, EventId eventId) {
    @JsonCreator
    ParticipationDTO(@JsonProperty("participationId") String participationId, @JsonProperty("eventId") String eventId) {
        this(new ParticipantId(participationId), new EventId(eventId));
    }
}
