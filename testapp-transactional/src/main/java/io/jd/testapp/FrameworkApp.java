package io.jd.testapp;

import io.jd.framework.BeanProvider;
import io.jd.framework.BeanProviderFactory;

public class FrameworkApp {
    public static void main(String[] args) {
        BeanProvider provider = BeanProviderFactory.getInstance();
        ParticipationService participationService = provider.provide(ParticipationService.class);
        participationService.participate(new ParticipantId(), new EventId());
    }
}
