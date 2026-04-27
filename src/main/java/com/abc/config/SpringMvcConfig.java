package com.abc.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

//配置springmvc
@Configuration
@ComponentScan("com.abc")
@EnableWebMvc
public class SpringMvcConfig {
}
