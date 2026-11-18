package ma.bench.jersey.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

@ApplicationPath("/api")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                ma.bench.jersey.resource.CategoryResource.class,
                ma.bench.jersey.resource.ItemResource.class,
                ma.bench.jersey.resource.CategoryItemsResource.class,
                ma.bench.jersey.resource.MetriquesResource.class,
                JacksonConfig.class,
                ConflitMapper.class);
    }
}