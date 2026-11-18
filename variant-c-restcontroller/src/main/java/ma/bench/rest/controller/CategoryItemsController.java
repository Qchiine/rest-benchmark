package ma.bench.rest.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ma.bench.rest.dto.PageResponse;
import ma.bench.rest.entity.Item;
import ma.bench.rest.repository.ItemRepository;

@RestController
@RequestMapping("/api/categories")
public class CategoryItemsController {

    private final ItemRepository itemRepository;

    public CategoryItemsController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping("/{id}/items")
    public PageResponse<Item> itemsDeLaCategorie(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200), Sort.by("id"));
        Page<Item> resultat = itemRepository.findByCategoryIdWithCategory(id, pageable);
        return PageResponse.of(resultat);
    }
}
