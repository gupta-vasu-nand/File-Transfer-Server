package org.vng.filetransferserver.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Serve index.html as the root page
        registry.addViewController("/").setViewName("forward:/index.html")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Serve static resources from classpath:/static/
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(3600)

        // Also serve static resources from the file system if needed (optional)
        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:./uploads/")
    }
}