package com.pullit;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        // 로컬 개발시에만 .env 파일 사용, 프로덕션은 시스템 환경변수 사용
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()  // .env 파일이 없어도 실행 가능
                .load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        SpringApplication.run(BackendApplication.class, args);

    }


}


