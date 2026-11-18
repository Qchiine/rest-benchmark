package ma.bench.datarest.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Data REST expose nativement /items/search/byCategory?categoryId=.
 * Le protocole de benchmark impose /items?categoryId= pour comparer les trois
 * variantes sur des URLs identiques. Ce filtre reecrit uniquement la collection
 * items portant ce parametre et laisse toute autre route intacte.
 */
@Component
@Order(1)
public class CategoryFilterUrlRewrite extends OncePerRequestFilter {

    private static final String ITEMS_COLLECTION = "/api/items";
    private static final String ITEMS_SEARCH = "/api/items/search/byCategory";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String categoryId = request.getParameter("categoryId");

        if (categoryId != null && ITEMS_COLLECTION.equals(uri)) {
            String query = request.getQueryString();
            String forwarded = ITEMS_SEARCH + "?" + (query == null ? "" : query);
            request.getRequestDispatcher(forwarded).forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
