package com.siemens.internship;

import com.siemens.internship.exception.DataProcessingException;
import com.siemens.internship.exception.ResourceNotFoundException;
import com.siemens.internship.model.Item;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    private final ExecutorService executor; // Used to run asynchronous tasks.

    public ItemService(@Value("${item.processing.threads:10}") int threadCount) {
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public boolean existsById(Long id) {
        return itemRepository.existsById(id);
    }

    public Item save(Item item) {
        try {
            return itemRepository.save(item);
        } catch (DataAccessException e) {
            throw new DataProcessingException("Failed to save item", e);
        }
    }

    public void deleteById(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item not found with id: " + id);
        }
        try {
            itemRepository.deleteById(id);
        } catch (DataAccessException e) {
            throw new DataProcessingException("Failed to delete item with id: " + id, e);
        }
    }

    /**
     * Original implementation had several issues:
     * 1. Returned incomplete results
     * 2. Thread safety violations
     * 3. Poor error handling
     * 4. Risky sleep interruption
     */
    @Async // Spring's @Async ensures this method call is executed asynchronously
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        // Create a list of CompletableFuture, each representing the processing of a single item
        List<CompletableFuture<Item>> futures = itemIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> processSingleItem(id), executor))
                .toList();

        // Return a CompletableFuture that completes when all tasks are done
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(voidResult ->
                        // Collect the results from all completed futures
                        // Filter out nulls, which represent items that were not found or failed to process
                        futures.stream()
                                .map(future -> {
                                    try {
                                        return future.join(); // .get() can throw InterruptedException or ExecutionException
                                    } catch (Exception e) {
                                        return null; // Treat as a failed item
                                    }
                                })
                                .filter(Objects::nonNull) // Filter out items that failed processing (returned as null)
                                .collect(Collectors.toList())
                );
    }

    /**
     * Processes a single item: fetches, updates status, simulates work, and saves.
     *
     * @param id The ID of the item to process.
     * @return The processed Item if successful, or null if the item was not found or an error occurred.
     */
    private Item processSingleItem(Long id) {
        try {
            Thread.sleep(100);

            Optional<Item> itemOptional = itemRepository.findById(id);
            if (itemOptional.isEmpty()) {
                return null; // Item not found, cannot process
            }

            Item item = itemOptional.get();
            item.setStatus("PROCESSED");
            Item savedItem = itemRepository.save(item); // Save the item and get the entity

            return savedItem;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null; // Indicate failure for this item
        }
    }

    // Gracefully stops thread pool during Spring context shutdown
    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
    }

}
