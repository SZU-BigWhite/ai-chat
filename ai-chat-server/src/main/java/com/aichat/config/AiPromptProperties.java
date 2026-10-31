package com.aichat.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 提示词位置支持 classpath 或外部 file 资源，启动时加载。 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.ai.prompts")
public class AiPromptProperties {
    @NotNull
    private Resource extract = new ClassPathResource("prompts/extract.txt");

    @NotNull
    private Resource expand = new ClassPathResource("prompts/expand.txt");

    public Resource getExtract() {
        return extract;
    }

    public void setExtract(Resource extract) {
        this.extract = extract;
    }

    public Resource getExpand() {
        return expand;
    }

    public void setExpand(Resource expand) {
        this.expand = expand;
    }

}
