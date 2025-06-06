package cn.org.shelly.picporter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerConfig {
    private static final String API_TILE = "基于springboot的图片上传相关助手——PicPorter";

    /**
     * 配置Swagger2的Docket的Bean实例
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                // select()：生成 API 文档的选择器，用于指定要生成哪些 API 文档
                .select()
                // apis()：指定要生成哪个包下的 API 文档
                .apis(RequestHandlerSelectors.basePackage("cn.org.shelly.picporter.controller"))
                // paths()：指定要生成哪个 URL 匹配模式下的 API 文档。这里使用 PathSelectors.any()，表示生成所有的 API 文档。
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 开放api
     *
     * @return {@link OpenAPI}
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(API_TILE)
                        .contact(new Contact().name("Shelly").url("http://localhost:8080/").email( "40505282@qq.com"))
                        .description(API_TILE)
                        .version("1.0"));
    }
}
