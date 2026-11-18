package ma.bench.datarest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import ma.bench.datarest.entity.Item;

@RepositoryRestResource(path = "items", collectionResourceRel = "items", itemResourceRel = "item")
public interface ItemRepository extends JpaRepository<Item, Long> {

    @RestResource(path = "byCategory", rel = "byCategory")
    Page<Item> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @RestResource(exported = false)
    @Query("select i from Item i join fetch i.category where i.id = :id")
    Item findByIdWithCategory(@Param("id") Long id);

    @Override
    @RestResource(exported = false)
    void deleteAll();
}
