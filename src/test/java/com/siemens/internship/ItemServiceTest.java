package com.siemens.internship;

import com.siemens.internship.exception.DataProcessingException;
import com.siemens.internship.exception.ResourceNotFoundException;
import com.siemens.internship.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    private ItemService itemService;

    @BeforeEach
    void init() {
        itemService = new ItemService(5); // 5 threads for test
        ReflectionTestUtils.setField(itemService, "itemRepository", itemRepository);
    }

    @Test
    void testFindAll() {
        when(itemRepository.findAll()).thenReturn(List.of(new Item()));

        List<Item> items = itemService.findAll();

        assertEquals(1, items.size());
    }

    @Test
    void testSave_Success() {
        Item item = new Item();
        when(itemRepository.save(item)).thenReturn(item);

        Item saved = itemService.save(item);

        assertEquals(item, saved);
    }

    @Test
    void testSave_ThrowsException() {
        Item item = new Item();
        when(itemRepository.save(any())).thenThrow(new DataAccessException("DB error") {});

        assertThrows(DataProcessingException.class, () -> itemService.save(item));
    }

    @Test
    void testDeleteById_Success() {
        when(itemRepository.existsById(1L)).thenReturn(true);
        doNothing().when(itemRepository).deleteById(1L);

        assertDoesNotThrow(() -> itemService.deleteById(1L));
    }

    @Test
    void testDeleteById_NotFound() {
        when(itemRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> itemService.deleteById(1L));
    }

    @Test
    void testProcessItemsAsync() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(new Item()));
        when(itemRepository.save(any())).thenReturn(new Item());

        CompletableFuture<List<Item>> result = itemService.processItemsAsync();
        List<Item> processed = result.get(3, TimeUnit.SECONDS);

        assertEquals(2, processed.size());
    }
}

