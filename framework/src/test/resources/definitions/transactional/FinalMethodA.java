package io.jd.framework.definitions;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public class FinalMethodA {

    @Transactional
    public final void save() {}

}
