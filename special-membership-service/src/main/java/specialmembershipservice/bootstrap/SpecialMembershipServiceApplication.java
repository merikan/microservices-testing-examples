package specialmembershipservice.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import specialmembershipservice.port.incoming.adapter.resources.SpecialMembershipsResource;
import specialmembershipservice.port.outgoing.adapter.creditscore.CreditScoreService;
import specialmembershipservice.port.outgoing.adapter.eventpublisher.EventPublisher;

import javax.ws.rs.client.WebTarget;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;

public class SpecialMembershipServiceApplication extends Application<SpecialMembershipServiceConfiguration> {

    @Override
    public void run(SpecialMembershipServiceConfiguration configuration, Environment environment) throws Exception {
        configureObjectMapper(environment.getObjectMapper());
        registerResources(configuration, environment);
    }

    private void configureObjectMapper(ObjectMapper objectMapper) {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(WRITE_DATES_AS_TIMESTAMPS);
    }

    private void registerResources(SpecialMembershipServiceConfiguration configuration, Environment environment) {
        CreditScoreService creditScoreService = createCreditScoreService(configuration, environment);
        EventPublisher eventPublisher = createEventPublisher(configuration, environment);
        environment.jersey().register(new SpecialMembershipsResource(creditScoreService, eventPublisher));
    }

    private CreditScoreService createCreditScoreService(SpecialMembershipServiceConfiguration configuration, Environment environment) {
        WebTarget webTarget = new JerseyClientBuilder(environment)
                .using(environment.getObjectMapper().copy())
                .build(getName())
                .property(CONNECT_TIMEOUT, 1000)
                .property(READ_TIMEOUT, 2000)
                .target(configuration.getCreditScoreServiceUrl());
        return new CreditScoreService(webTarget);
    }

    private EventPublisher createEventPublisher(SpecialMembershipServiceConfiguration configuration,
                                                Environment environment) {
        return new EventPublisher(
                configuration.getEventPublisher().getTopic(),
                configuration.getEventPublisher().getConfigs(),
                environment.getObjectMapper());
    }
}
