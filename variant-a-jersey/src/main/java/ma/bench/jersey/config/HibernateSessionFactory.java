package ma.bench.jersey.config;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import ma.bench.jersey.entity.Category;
import ma.bench.jersey.entity.Item;

/**
 * SessionFactory de la variante A.
 *
 * Le pool est celui fourni par Hibernate (HikariCPConnectionProvider), avec le
 * meme dimensionnement que les variantes C et D (minimum 10, maximum 50) afin
 * que les mesures de concurrence restent comparables.
 */
public final class HibernateSessionFactory {

    private static volatile SessionFactory sessionFactory;

    private HibernateSessionFactory() {
    }

    public static SessionFactory get() {
        if (sessionFactory == null) {
            synchronized (HibernateSessionFactory.class) {
                if (sessionFactory == null) {
                    sessionFactory = construire();
                }
            }
        }
        return sessionFactory;
    }

    private static SessionFactory construire() {
        String hote = System.getenv().getOrDefault("BENCH_DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("BENCH_DB_PORT", "5433");

        Properties proprietes = new Properties();
        proprietes.put(Environment.CONNECTION_PROVIDER,
                "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        proprietes.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        proprietes.put(Environment.HBM2DDL_AUTO, "none");
        proprietes.put(Environment.SHOW_SQL, "false");
        proprietes.put("hibernate.jdbc.batch_size", "50");
        proprietes.put(Environment.ORDER_INSERTS, "true");
        proprietes.put(Environment.ORDER_UPDATES, "true");
        proprietes.put("hibernate.hikari.jdbcUrl", "jdbc:postgresql://" + hote + ":" + port + "/bench");
        proprietes.put("hibernate.hikari.username", "bench");
        proprietes.put("hibernate.hikari.password", "bench");
        proprietes.put("hibernate.hikari.minimumIdle", "10");
        proprietes.put("hibernate.hikari.maximumPoolSize", "50");
        proprietes.put("hibernate.hikari.poolName", "bench-pool");
        proprietes.put("hibernate.hikari.connectionTimeout", "30000");

        return new Configuration()
                .addAnnotatedClass(Category.class)
                .addAnnotatedClass(Item.class)
                .addProperties(proprietes)
                .buildSessionFactory();
    }
}