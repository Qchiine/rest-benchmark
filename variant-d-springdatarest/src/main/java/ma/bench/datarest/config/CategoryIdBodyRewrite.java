package ma.bench.datarest.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Data REST attend une URI de ressource pour une association
 * ("category": "http://host/api/categories/2") et non un identifiant brut.
 * Le protocole de benchmark impose le meme corps JSON aux trois variantes
 * ("categoryId": 2). Ce filtre traduit uniquement ce champ sur les ecritures
 * d'items, sans modifier le comportement d'exposition automatique.
 */
@Component
@Order(2)
public class CategoryIdBodyRewrite extends OncePerRequestFilter {

    private static final Pattern CATEGORY_ID =
            Pattern.compile("\"categoryId\"\\s*:\\s*(\\d+)");

    private static final Pattern ITEM_PATH = Pattern.compile("^/api/items(?:/\\d+)?$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String methode = request.getMethod();
        boolean ecriture = "POST".equals(methode) || "PUT".equals(methode) || "PATCH".equals(methode);

        if (!ecriture || !ITEM_PATH.matcher(request.getRequestURI()).matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        String corps = lireCorps(request);
        if (corps == null || corps.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Matcher matcher = CATEGORY_ID.matcher(corps);
        if (!matcher.find()) {
            filterChain.doFilter(new CorpsInjecte(request, corps), response);
            return;
        }

        String base = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + "/api/categories/";
        String reecrit = matcher.replaceAll("\"category\":\"" + Matcher.quoteReplacement(base) + "$1\"");

        filterChain.doFilter(new CorpsInjecte(request, reecrit), response);
    }

    private String lireCorps(HttpServletRequest request) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                builder.append(ligne);
            }
        }
        return builder.toString();
    }

    private static final class CorpsInjecte extends HttpServletRequestWrapper {

        private final byte[] corps;

        CorpsInjecte(HttpServletRequest request, String corps) {
            super(request);
            this.corps = corps.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public int getContentLength() {
            return corps.length;
        }

        @Override
        public long getContentLengthLong() {
            return corps.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(corps);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return source.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("lecture synchrone uniquement");
                }

                @Override
                public int read() {
                    return source.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(corps), StandardCharsets.UTF_8));
        }
    }
}