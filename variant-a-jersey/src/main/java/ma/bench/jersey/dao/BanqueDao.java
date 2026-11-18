package ma.bench.jersey.dao;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;

import ma.bench.jersey.entity.Category;
import ma.bench.jersey.entity.Item;

public class BanqueDao {

    private final SessionFactory sessionFactory;

    public BanqueDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Flush explicite avant commit : l'exception metier (contrainte d'unicite)
     * remonte alors directement, au lieu d'etre masquee par la fermeture de session.
     */
    public <T> T dansTransaction(FonctionSession<T> fonction) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            T resultat = fonction.appliquer(session);
            session.flush();
            transaction.commit();
            return resultat;
        } catch (RuntimeException exception) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (RuntimeException ignore) {
                    // la cause d'origine est plus utile que l'echec du rollback
                }
            }
            throw exception;
        } finally {
            session.close();
        }
    }

    public <T> T enLecture(FonctionSession<T> fonction) {
        try (Session session = sessionFactory.openSession()) {
            return fonction.appliquer(session);
        }
    }

    public List<Category> categoriesPaginees(int page, int size) {
        return enLecture(session -> session
                .createQuery("select c from Category c order by c.id", Category.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList());
    }

    public long totalCategories() {
        return enLecture(session -> session
                .createQuery("select count(c) from Category c", Long.class)
                .getSingleResult());
    }

    public Optional<Category> categorieParId(Long id) {
        return enLecture(session -> Optional.ofNullable(session.find(Category.class, id)));
    }

    public List<Item> itemsPaginees(int page, int size) {
        return enLecture(session -> session
                .createQuery("select i from Item i join fetch i.category order by i.id", Item.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList());
    }

    public long totalItems() {
        return enLecture(session -> session
                .createQuery("select count(i) from Item i", Long.class)
                .getSingleResult());
    }

    public List<Item> itemsParCategorie(Long categoryId, int page, int size) {
        return enLecture(session -> session
                .createQuery("select i from Item i join fetch i.category where i.category.id = :cid order by i.id",
                        Item.class)
                .setParameter("cid", categoryId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList());
    }

    public long totalItemsParCategorie(Long categoryId) {
        return enLecture(session -> session
                .createQuery("select count(i) from Item i where i.category.id = :cid", Long.class)
                .setParameter("cid", categoryId)
                .getSingleResult());
    }

    public Optional<Item> itemParId(Long id) {
        return enLecture(session -> {
            Query<Item> query = session
                    .createQuery("select i from Item i join fetch i.category where i.id = :id", Item.class)
                    .setParameter("id", id);
            return query.uniqueResultOptional();
        });
    }

    @FunctionalInterface
    public interface FonctionSession<T> {
        T appliquer(Session session);
    }
}