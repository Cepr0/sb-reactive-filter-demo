package io.github.cepr0.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@EnableAsync
@RestController
@EnableReactiveMongoRepositories
@SpringBootApplication
public class Application {

	private final UserRepo userRepo;
	private final UserService userService;

	public Application(UserRepo userRepo, UserService userService) {
		this.userRepo = userRepo;
		this.userService = userService;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@GetMapping("/")
	public Mono<?> get(@RequestHeader("token") String token, ServerWebExchange exchange) {
		Object tokenAttribute = exchange.getAttributes().get("_token");
		return Mono.just(Map.of("token", token, "_token", tokenAttribute != null ? tokenAttribute : ""));
	}

	@GetMapping("/users")
	public Flux<?> getAllUsers() {
		log.info("[i] Get all users...");
		return userRepo.findAll();
	}

	@Bean
	public WebFilter filter() {
		return (exchange, chain) -> {
			ServerHttpRequest req = exchange.getRequest();
			String uri = req.getURI().toString();
			log.info("[i] Got request: {}", uri);

			var headers = req.getHeaders();
			List<String> tokenList = headers.get("token");

			if (tokenList != null && tokenList.get(0) != null) {
				String token = tokenList.get(0);
				log.info("[i] Find a user by token {}", token);
				return userRepo.findByToken(token)
						.map(user -> process(exchange, uri, token, user))
						.then(chain.filter(exchange));
			} else {
				String token = UUID.randomUUID().toString();
				log.info("[i] Create a new user with token {}", token);
				return userRepo.save(new User(token))
						.map(user -> process(exchange, uri, token, user))
						.then(chain.filter(exchange));
			}
		};
	}

	private User process(ServerWebExchange exchange, String uri, String token, User user) {
		exchange.getAttributes().put("_token", token);
		userService.updateUserStat(uri, user); // async call
		return user;
	}
}
