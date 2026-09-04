package com.lingko.lingko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 백엔드와 component graph를 시작한다.
 *
 * 설정과 업무 동작이 각 소유 module에 남도록 entry point는 의도적으로 작게 유지한다.
 */
@SpringBootApplication
public class LingkoApplication {

	public static void main(String[] args) {
		SpringApplication.run(LingkoApplication.class, args);
	}

}
