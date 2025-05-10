package com.siemens.internship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Interface for Repository that implements all CRUD functionalities that come with JpaRepository ( for entity: Item and primary key: Long)
// additionally it adds findAllIds() method
public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("SELECT id FROM Item")
    List<Long> findAllIds();
}
