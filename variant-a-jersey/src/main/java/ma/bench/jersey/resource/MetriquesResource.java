package ma.bench.jersey.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import ma.bench.jersey.config.MetriquesPrometheus;

/**
 * Expose le meme chemin que Spring Boot Actuator (/actuator/prometheus)
 * pour que Prometheus scrape les trois variantes avec une seule configuration.
 */
@Path("/actuator/prometheus")
public class MetriquesResource {

    @GET
    @Produces("text/plain; version=0.0.4; charset=utf-8")
    public String scrape() {
        return MetriquesPrometheus.scrape();
    }
}
