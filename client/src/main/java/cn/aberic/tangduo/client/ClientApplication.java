package cn.aberic.tangduo.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication(scanBasePackages = {"cn.aberic.tangduo.client"})
@EnableScheduling
@EnableAsync
public class ClientApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(ClientApplication.class, args);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(-1);
        }

    }

}
