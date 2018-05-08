package edu.sjsu.cmpe282.s3url;

import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Method;

@SpringBootApplication
@ComponentScan({"edu.sjsu.cmpe282.s3url"})
@EntityScan({"edu.sjsu.cmpe282.s3url"})
@EnableDynamoDBRepositories({"edu.sjsu.cmpe282.s3url"})
@EnableCaching
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);

	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("**")
                        .allowedOrigins("*")
                        .allowedMethods("PUT", "DELETE", "GET", "POST", "OPTIONS")
                        .allowedHeaders("content-type", "authorization", "accept", "x-forwarded-user");
			}
		};
	}

    @Bean("keygen")
    public KeyGenerator customKeyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object o, Method method, Object... objects) {
                if (objects.length >= 2) {
                    UrlMap map = (UrlMap)objects[1];
                    return WebController.getHash(map.getoURL());
                }
                return "";
            }
        };
    }
}
