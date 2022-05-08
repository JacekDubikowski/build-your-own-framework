package io.jd.framework.definitions;

import jakarta.inject.Singleton;
import javax.transaction.Transactional;

@Singleton
public class StaticA {

    @Transactional
    static void save() {}

}
