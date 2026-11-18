package ma.bench.datarest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import ma.bench.datarest.entity.Category;

@RepositoryRestResource(path = "categories", collectionResourceRel = "categories", itemResourceRel = "category")
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Override
    @RestResource(exported = false)
    void deleteAll();
}
