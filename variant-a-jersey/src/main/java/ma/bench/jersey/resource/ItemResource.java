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
import ma.bench.jersey.dto.ItemPayload;
import ma.bench.jersey.dto.PageResponse;
import ma.bench.jersey.entity.Category;
import ma.bench.jersey.entity.Item;

@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemResource {

    private final BanqueDao dao = new BanqueDao(HibernateSessionFactory.get());

    @GET
    public PageResponse<Item> liste(
            @QueryParam("categoryId") Long categoryId,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        int p = page == null ? 0 : Math.max(page, 0);
        int s = clamp(size);

        if (categoryId == null) {
            return PageResponse.of(dao.itemsPaginees(p, s), p, s, dao.totalItems());
        }
        return PageResponse.of(dao.itemsParCategorie(categoryId, p, s), p, s, dao.totalItemsParCategorie(categoryId));
    }

    @GET
    @Path("/{id}")
    public Response detail(@PathParam("id") Long id) {
        return dao.itemParId(id)
                .map(item -> Response.ok(item).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response creer(ItemPayload payload) {
        if (!payload.estComplet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return dao.dansTransaction(session -> {
            Category category = session.find(Category.class, payload.categoryId());
            if (category == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Item item = new Item();
            item.setSku(payload.sku());
            item.setName(payload.name());
            item.setPrice(payload.price());
            item.setStock(payload.stock());
            item.setCategory(category);
            item.setUpdatedAt(Instant.now());
            session.persist(item);
            return Response.status(Response.Status.CREATED).entity(item).build();
        });
    }

    @PUT
    @Path("/{id}")
    public Response modifier(@PathParam("id") Long id, ItemPayload payload) {
        if (!payload.estComplet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return dao.dansTransaction(session -> {
            Item item = session.find(Item.class, id);
            if (item == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Category category = session.find(Category.class, payload.categoryId());
            if (category == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            item.setSku(payload.sku());
            item.setName(payload.name());
            item.setPrice(payload.price());
            item.setStock(payload.stock());
            item.setCategory(category);
            item.setUpdatedAt(Instant.now());
            session.merge(item);
            return Response.ok(item).build();
        });
    }

    @DELETE
    @Path("/{id}")
    public Response supprimer(@PathParam("id") Long id) {
        Boolean supprime = dao.dansTransaction(session -> {
            Item item = session.find(Item.class, id);
            if (item == null) {
                return false;
            }
            session.remove(item);
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
