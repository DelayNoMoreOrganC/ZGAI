package com.lawfirm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 律所智能案件管理系统 - 主应用类
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@MapperScan("com.lawfirm.mapper")
public class LawFirmApplication {

    public static void main(String[] args) {
        SpringApplication.run(LawFirmApplication.class, args);
        System.out.println("\n" +
                "   ____                              __      ____       __       _       __       \n" +
                "  / __ \\____ _____  ___  __________/ /_    /  _/___   / /      | |     / /___ _      \n" +
                " / /_/ / __ `/ __ \\/ _ \\/ ___/ ___/ __/    / // __ \\ / /       | | /| / / __ \\/\n" +
                "/ _, _/ /_/ / / / /  __/ /  (__  ) /_    _/ // / / // /        | |/ |/ / /_/ /      \n" +
                "/_/ |_|\\__,_/_/ /_/\\___/_/  /____/\\__/  /___/_/ /_//_/         |__/|__/\\____/      \n" +
                "                                                                                   \n" +
                "律所智能案件管理系统启动成功！\n" +
                "访问地址: http://localhost:8080/api\n");
    }
}
