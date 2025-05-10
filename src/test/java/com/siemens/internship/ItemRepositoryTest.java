package com.siemens.internship;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void testSaveAndFindById() {
        Item item = new Item(null, "Repo Test", "desc", "NEW", "repo@test.com");
        Item saved = itemRepository.save(item);

        Optional<Item> found = itemRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Repo Test", found.get().getName());
    }

    @Test
    void testFindAllIds() {
        itemRepository.save(new Item(null, "One", "desc", "NEW", "a@b.com"));
        itemRepository.save(new Item(null, "Two", "desc", "NEW", "b@c.com"));

        List<Long> ids = itemRepository.findAllIds();

        assertEquals(2, ids.size());
    }
}

