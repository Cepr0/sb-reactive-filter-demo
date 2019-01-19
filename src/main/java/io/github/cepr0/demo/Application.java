package io.github.cepr0.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@EnableReactiveMongoRepositories
@SpringBootApplication
public class Application {

	private final UserRepo userRepo;
	private final PageViewRepo pageViewRepo;

	public Application(UserRepo userRepo, final PageViewRepo pageViewRepo) {
		this.userRepo = userRepo;
		this.pageViewRepo = pageViewRepo;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@GetMapping("/")
	public Mono<?> get(ServerWebExchange exchange) {
		Object token = exchange.getAttributes().get("_token");
		log.info("[i] Controller: handling 'root' request. Token attribute is '{}'", token);
		return Mono.just(Map.of("_token", token != null ? token : "null"));
	}

	@GetMapping("/users")
	public Flux<?> getAllUsers(ServerWebExchange exchange) {
		Object token = exchange.getAttribute("_token");
		log.info("[i] Controller: handling 'get all users' request. Token attribute is '{}'", token);
		return userRepo.findAll();
	}

	@Bean
	public WebFilter filter() {
		return (exchange, chain) -> {
			ServerHttpRequest req = exchange.getRequest();
			String uri = req.getURI().toString();
			log.info("[i] Web Filter: received the request: {}", uri);

			var headers = req.getHeaders();
			List<String> tokenList = headers.get("token");

			if (tokenList != null && tokenList.get(0) != null) {
				String token = tokenList.get(0);
				Mono<User> foundUser = userRepo
						.findByToken(token)
						.doOnNext(user -> log.info("[i] Web Filter: {} has been found", user));
				return updateUserStat(foundUser, exchange, chain, uri);
			} else {
				String token = UUID.randomUUID().toString();
				Mono<User> createdUser = userRepo
						.save(new User(token))
						.doOnNext(user -> log.info("[i] Web Filter: a new {} has been created", user));
				return updateUserStat(createdUser, exchange, chain, uri);
			}
		};
	}

	private Mono<Void> updateUserStat(Mono<User> userMono, ServerWebExchange exchange, WebFilterChain chain, String uri) {
		return userMono
				.doOnNext(user -> exchange.getAttributes().put("_token", user.getToken()))
				.doOnNext(u -> {
					String token = exchange.getAttribute("_token");
					log.info("[i] Web Filter: token attribute has been set to '{}'", token);
				})
				.delayElement(Duration.ofSeconds(1)) // emulates a delay while updating the user
				.flatMap(user -> pageViewRepo.save(new PageView(uri)).flatMap(user::addPageView).flatMap(userRepo::save))
				.doOnNext(user -> {
					int numberOfPages = 0;
					List<PageView> pageViews = user.getPageViews();
					if (pageViews != null) {
						numberOfPages = pageViews.size();
					}
					log.info("[i] Web Filter: {} has been updated. Number of pages: {}", user, numberOfPages);
				})
				.then(chain.filter(exchange));
	}
}
