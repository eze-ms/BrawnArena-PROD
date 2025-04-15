package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterRepository extends ReactiveMongoRepository<Character, String> {
}