package ma.bench.jersey.config;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Registre Prometheus de la variante A.
 *
 * La variante A n'embarque pas Spring Boot Actuator : ce registre expose les
 * memes familles de metriques JVM que les variantes C et D, afin que le tableau
 * T3 (CPU, heap, GC, threads) reste comparable entre les trois implementations.
 */
public final class MetriquesPrometheus {

    private static final PrometheusMeterRegistry REGISTRE = construire();

    private MetriquesPrometheus() {
    }

    private static PrometheusMeterRegistry construire() {
        PrometheusMeterRegistry registre = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        new ClassLoaderMetrics().bindTo(registre);
        new JvmMemoryMetrics().bindTo(registre);
        new JvmGcMetrics().bindTo(registre);
        new JvmThreadMetrics().bindTo(registre);
        new ProcessorMetrics().bindTo(registre);

        return registre;
    }

    public static PrometheusMeterRegistry registre() {
        return REGISTRE;
    }

    public static String scrape() {
        return REGISTRE.scrape();
    }
}
