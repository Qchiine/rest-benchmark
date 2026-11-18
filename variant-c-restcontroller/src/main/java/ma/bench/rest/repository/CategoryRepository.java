package ma.bench.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.bench.rest.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
