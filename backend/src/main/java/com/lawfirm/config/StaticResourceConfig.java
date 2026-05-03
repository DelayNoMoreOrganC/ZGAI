package com.lawfirm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 静态资源配置 - 提供前端SPA静态文件服务
 * 
 * 构建前端后，访问 http://localhost:8080 即可加载完整前端页面
 * 所有非API路径将fallback到 index.html（SPA路由支持）
 * 
 * 构建命令: cd frontend && npm run build
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final String[] STATIC_LOCATIONS = {
        "file:/Users/juno/ZGAI/frontend/dist/",
        "classpath:/static/"
    };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源（JS/CSS/图片等）- 强缓存1年，内容hash化
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(STATIC_LOCATIONS)
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

        // 其他静态资源（favicon等）- 弱缓存
        registry.addResourceHandler("/favicon.svg", "/favicon.ico")
                .addResourceLocations(STATIC_LOCATIONS)
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .resourceChain(true);

        // SPA fallback: 所有非API路径返回 index.html
        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_LOCATIONS)
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());
    }

    /**
     * SPA fallback resolver: 如果请求的资源不存在，返回 index.html
     */
    private static class SpaFallbackResourceResolver extends PathResourceResolver {

        private static final String INDEX_HTML = "index.html";

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            // 先尝试正常解析
            Resource resource = super.getResource(resourcePath, location);
            if (resource != null && resource.exists() && resource.isReadable()) {
                return resource;
            }
            // 不存在则返回 index.html（SPA fallback）
            Resource indexResource = super.getResource(INDEX_HTML, location);
            if (indexResource != null && indexResource.exists()) {
                return indexResource;
            }
            return null;
        }
    }
}
