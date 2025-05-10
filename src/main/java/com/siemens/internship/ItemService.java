package com.siemens.internship;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async // Spring's @Async ensures this method call is executed asynchronously
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        // Create a list of CompletableFuture, each representing the processing of a single item.
        // We use supplyAsync because each task will return a result (the processed Item or null).
        List<CompletableFuture<Item>> futures = itemIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> processSingleItem(id), executor))
                .collect(Collectors.toList());

        // Instead of blocking with join(), we return a CompletableFuture that completes
        // when all tasks are done. This allows non-blocking async behavior.
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(voidResult ->
                        // Collect the results from all completed futures.
                        // We filter out nulls, which represent items that were not found or failed to process.
                        futures.stream()
                                .map(future -> {
                                    try {
                                        return future.get(); // .get() can throw InterruptedException or ExecutionException
                                    } catch (Exception e) {
                                        return null; // Treat as a failed item
                                    }
                                })
                                .filter(item -> item != null) // Filter out items that failed processing (returned as null)
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
            if (!itemOptional.isPresent()) {
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

    // It's good practice to ensure the ExecutorService is properly shut down when the application stops.
    // If this ExecutorService were managed by Spring (e.g., as a @Bean with a destroyMethod),
    // Spring would handle its lifecycle. For a static executor, manual shutdown might be needed
    // via a @PreDestroy method in the service or a ApplicationListener<ContextClosedEvent>.
    // For example:
    // @javax.annotation.PreDestroy
    // public void shutdownExecutor() {
    //     logger.info("Shutting down item processing executor service.");
    //     executor.shutdown();
    //     try {
    //         if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
    //             executor.shutdownNow();
    //         }
    //     } catch (InterruptedException e) {
    //         executor.shutdownNow();
    //         Thread.currentThread().interrupt();
    //     }
    // }

    // Gracefully stops thread pool during Spring context shutdown
    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
    }

}
