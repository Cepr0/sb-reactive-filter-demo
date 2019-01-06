package io.github.cepr0.demo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface PageViewRepo extends ReactiveMongoRepository<PageView, String> {
}
