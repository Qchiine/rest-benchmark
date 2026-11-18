package ma.bench.jersey.resource;

import java.time.Instant;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ma.bench.jersey.config.HibernateSessionFactory;
import ma.bench.jersey.dao.BanqueDao;
import ma.bench.jersey.dto.CategoryPayload;
import ma.bench.jersey.dto.PageResponse;
import ma.bench.jersey.entity.Category;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    private final BanqueDao dao = new BanqueDao(HibernateSessionFactory.get());

    @GET
    public PageResponse<Category> liste(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        int p = page == null ? 0 : Math.max(page, 0);
        int s = clamp(size);
        List<Category> contenu = dao.categoriesPaginees(p, s);
        long total = dao.totalCategories();
        return PageResponse.of(contenu, p, s, total);
    }

    @GET
    @Path("/{id}")
    public Response detail(@PathParam("id") Long id) {
        return dao.categorieParId(id)
                .map(category -> Response.ok(category).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response creer(CategoryPayload payload) {
        if (!payload.estComplet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Category created = dao.dansTransaction(session -> {
            Category category = new Category();
            category.setCode(payload.code());
            category.setName(payload.name());
            category.setUpdatedAt(Instant.now());
            session.persist(category);
            return category;
        });
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response modifier(@PathParam("id") Long id, CategoryPayload payload) {
        if (!payload.estComplet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return dao.dansTransaction(session -> {
            Category category = session.find(Category.class, id);
            if (category == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            category.setCode(payload.code());
            category.setName(payload.name());
            category.setUpdatedAt(Instant.now());
            session.merge(category);
            return Response.ok(category).build();
        });
    }

    @DELETE
    @Path("/{id}")
    public Response supprimer(@PathParam("id") Long id) {
        Boolean supprime = dao.dansTransaction(session -> {
            Category category = session.find(Category.class, id);
            if (category == null) {
                return false;
            }
            session.remove(category);
            return true;
        });
        return supprime
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }

    private int clamp(Integer size) {
        if (size == null) {
            return 50;
        }
        return Math.min(Math.max(size, 1), 200);
    }
}
