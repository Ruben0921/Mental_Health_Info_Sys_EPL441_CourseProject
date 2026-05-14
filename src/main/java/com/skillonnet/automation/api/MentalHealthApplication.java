package com.skillonnet.automation.api;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Jersey {@link ResourceConfig} registering API resources under {@code com.skillonnet.automation.api} and Jackson JSON.
 */
public class MentalHealthApplication extends ResourceConfig {

    public MentalHealthApplication() {
        packages("com.skillonnet.automation.api");
        register(JacksonFeature.class);
    }
}
