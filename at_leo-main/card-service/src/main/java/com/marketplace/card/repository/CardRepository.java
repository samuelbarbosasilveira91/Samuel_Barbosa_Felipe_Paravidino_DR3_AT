package com.marketplace.card.repository;

import com.marketplace.card.model.Card;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends MongoRepository<Card, String> {
    List<Card> findByGame(String game);
    List<Card> findByNameContainingIgnoreCase(String name);
}
