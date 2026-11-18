package ma.bench.rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ma.bench.rest.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Item> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Item> findByCategoryId(Long categoryId, Pageable pageable);

    @Query(value = "select i from Item i join fetch i.category where i.category.id = :categoryId",
           countQuery = "select count(i) from Item i where i.category.id = :categoryId")
    Page<Item> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("select i from Item i join fetch i.category where i.id = :id")
    Item findByIdWithCategory(@Param("id") Long id);
}