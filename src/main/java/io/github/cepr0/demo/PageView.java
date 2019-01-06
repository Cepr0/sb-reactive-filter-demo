package io.github.cepr0.demo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document
public class PageView {
	@Id private String id;
	private String URL;
	private LocalDateTime createdDate = LocalDateTime.now();

	public PageView(String URL) {
		this.URL = URL;
	}
}
