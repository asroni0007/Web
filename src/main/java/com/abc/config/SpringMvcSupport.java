package com.abc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class SpringMvcSupport extends WebMvcConfigurationSupport {

    //当访问/pages/？？？？时候，走/pages目录下的内容
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registration){
        registration.addResourceHandler("/pages/**").addResourceLocations("/pages/");
        registration.addResourceHandler("/js/**").addResourceLocations("/js/");
        registration.addResourceHandler("/element-ui/**").addResourceLocations("/element-ui/");
    }
}
