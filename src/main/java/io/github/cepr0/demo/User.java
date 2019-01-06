package io.github.cepr0.demo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Data
@Document
public class User {

	@Id private String id;
	@Indexed(unique = true) private String token;
	@DBRef private List<PageView> pageViews;

	public User(String token) {
		this.token = token;
	}

	public Mono<User> addPageView(PageView pageView) {
		if (pageView != null) {
			if (this.pageViews == null) this.pageViews = new ArrayList<>();
			this.pageViews.add(pageView);
		}
		return Mono.just(this);
	}
}
