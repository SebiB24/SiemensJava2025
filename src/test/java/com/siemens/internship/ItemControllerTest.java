package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Item item;

    @BeforeEach
    void setup() {
        item = new Item(1L, "Test Item", "Description", "NEW", "test@example.com");
    }

    @Test
    void testGetAllItems() throws Exception {
        when(itemService.findAll()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Item"));
    }

    @Test
    void testCreateItem_Valid() throws Exception {
        when(itemService.save(any(Item.class))).thenReturn(item);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testCreateItem_Invalid() throws Exception {
        item.setEmail("invalid_email");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void testGetItemById_Found() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Item"));
    }

    @Test
    void testGetItemById_NotFound() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateItem_Valid() throws Exception {
        when(itemService.existsById(1L)).thenReturn(true);
        when(itemService.save(any(Item.class))).thenReturn(item);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateItem_NotFound() throws Exception {
        when(itemService.existsById(1L)).thenReturn(false);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteItem_Success() throws Exception {
        when(itemService.existsById(1L)).thenReturn(true);
        doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteItem_NotFound() throws Exception {
        when(itemService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNotFound());
    }
}
