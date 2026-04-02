package cn.aberic.tangduo.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication(scanBasePackages = {"cn.aberic.tangduo.search"})
@EnableScheduling
public class SearchApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(SearchApplication.class, args);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(-1);
        }

    }

}
