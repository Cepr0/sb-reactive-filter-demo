package io.github.cepr0.demo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserRepo extends ReactiveMongoRepository<User, String> {
	Mono<User> findByToken(String token);
}
