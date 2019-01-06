package io.github.cepr0.demo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

	private final UserRepo userRepo;
	private final PageViewRepo pageViewRepo;

	public UserService(UserRepo userRepo, PageViewRepo pageViewRepo) {
		this.userRepo = userRepo;
		this.pageViewRepo = pageViewRepo;
	}

	@SneakyThrows
	@Async
	public void updateUserStat(String uri, User user) {
		log.info("[i] Start updating...");
		Thread.sleep(1000);
		pageViewRepo.save(new PageView(uri))
				.flatMap(user::addPageView)
				.blockOptional()
				.ifPresent(u -> userRepo.save(u).block());
		log.info("[i] User updated.");
	}
}
