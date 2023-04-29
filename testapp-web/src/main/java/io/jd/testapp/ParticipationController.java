package io.jd.testapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jd.framework.webapp.HttpMethod;
import io.jd.framework.webapp.Request;
import io.jd.framework.webapp.RequestHandle;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Map;

@Singleton
public class ParticipationController {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    @RequestHandle(value = "/participate", method = HttpMethod.POST)
    String participate(Request request) throws IOException {
        var participationDTO = objectMapper.readValue(request.body(), ParticipationDTO.class);
        participationService.participate(participationDTO.participantId(), participationDTO.eventId());
        return objectMapper.writeValueAsString(Map.of("accepted", participationDTO));
    }

}
