package ma.bench.rest.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import ma.bench.rest.dto.ItemRequest;
import ma.bench.rest.dto.PageResponse;
import ma.bench.rest.entity.Category;
import ma.bench.rest.entity.Item;
import ma.bench.rest.repository.CategoryRepository;
import ma.bench.rest.repository.ItemRepository;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public ItemController(ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public PageResponse<Item> liste(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, clamp(size), Sort.by("id"));
        Page<Item> resultat = categoryId == null
                ? itemRepository.findAll(pageable)
                : itemRepository.findByCategoryIdWithCategory(categoryId, pageable);
        return PageResponse.of(resultat);
    }

    @GetMapping("/{id}")
    public Item detail(@PathVariable Long id) {
        Item item = itemRepository.findByIdWithCategory(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item introuvable");
        }
        return item;
    }

    @PostMapping
    public ResponseEntity<Item> creer(@Valid @RequestBody ItemRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categorie inexistante"));
        Item item = new Item();
        item.setSku(request.sku());
        item.setName(request.name());
        item.setPrice(request.price());
        item.setStock(request.stock());
        item.setCategory(category);
        item.setUpdatedAt(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(itemRepository.save(item));
    }

    @PutMapping("/{id}")
    public Item modifier(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item introuvable"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categorie inexistante"));
        item.setSku(request.sku());
        item.setName(request.name());
        item.setPrice(request.price());
        item.setStock(request.stock());
        item.setCategory(category);
        item.setUpdatedAt(Instant.now());
        return itemRepository.save(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item introuvable");
        }
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private int clamp(int size) {
        return Math.min(Math.max(size, 1), 200);
    }
}
