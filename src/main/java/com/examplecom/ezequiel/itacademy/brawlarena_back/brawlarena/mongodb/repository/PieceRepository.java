package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface PieceRepository extends ReactiveMongoRepository<Piece, String> {
    Flux<Piece> findByIdIn(List<String> ids);

}
