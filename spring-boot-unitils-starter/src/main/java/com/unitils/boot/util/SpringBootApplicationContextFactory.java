package com.unitils.boot.util;

import org.springframework.context.ConfigurableApplicationContext;
import org.unitils.spring.util.ApplicationContextFactory;

import java.util.List;

/**
 * @Author: yangjianzhou
 * @Description:
 * @Date:Created in 2018-07-08
 */
public class SpringBootApplicationContextFactory implements ApplicationContextFactory {

    private static ConfigurableApplicationContext configurableApplicationContext ;

    public ConfigurableApplicationContext createApplicationContext(List<String> locations) {
        return configurableApplicationContext;
    }

    public static void setConfigurableApplicationContext(ConfigurableApplicationContext configurableApplicationContext) {
        SpringBootApplicationContextFactory.configurableApplicationContext = configurableApplicationContext;
    }
}
