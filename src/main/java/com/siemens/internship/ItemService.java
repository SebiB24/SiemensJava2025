package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    // Spring can also manage thread pools, but we keep this one and shut it down if needed
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Find all items
     */
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    /**
     * Find item by ID
     */
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    /**
     * Save or update item
     */
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    /**
     * Delete item by ID
     */
    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    /**
     * Asynchronously processes all items and returns a CompletableFuture containing the processed items.
     * Fixes applied:
     * - Thread-safe collection used
     * - CompletableFuture.allOf ensures all processing is completed
     * - Exceptions handled and logged
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());

        for (Long id : itemIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100); // Simulated processing time

                    itemRepository.findById(id).ifPresent(item -> {
                        item.setStatus("PROCESSED");
                        itemRepository.save(item);
                        processedItems.add(item);
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // best practice to preserve interrupt status
                    System.err.println("Thread interrupted: " + e.getMessage());
                } catch (Exception ex) {
                    System.err.println("Error processing item with id " + id + ": " + ex.getMessage());
                }
            }, executor);

            futures.add(future);
        }

        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processedItems);
    }
}
