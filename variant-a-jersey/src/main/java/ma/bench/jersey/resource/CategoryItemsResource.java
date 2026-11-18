package ma.bench.jersey.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import ma.bench.jersey.config.HibernateSessionFactory;
import ma.bench.jersey.dao.BanqueDao;
import ma.bench.jersey.dto.PageResponse;
import ma.bench.jersey.entity.Item;

@Path("/categories/{id}/items")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryItemsResource {

    private final BanqueDao dao = new BanqueDao(HibernateSessionFactory.get());

    @GET
    public PageResponse<Item> itemsDeLaCategorie(
            @PathParam("id") Long id,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        int p = page == null ? 0 : Math.max(page, 0);
        int s = size == null ? 50 : Math.min(Math.max(size, 1), 200);
        return PageResponse.of(
                dao.itemsParCategorie(id, p, s),
                p,
                s,
                dao.totalItemsParCategorie(id));
    }
}
