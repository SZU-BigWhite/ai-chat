package com.aichat.config;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 预置主题资源与新增主题的显示默认值；已有主题由数据库管理。 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.topic")
public class TopicProperties {

    private boolean seedEnabled = true;

    @NotNull
    private Resource seedLocation = new ClassPathResource("seed/topics.json");

    @NotNull
    @Pattern(regexp = BusinessConstants.HEX_COLOR_PATTERN)
    private String defaultColor = "#548b9d";

    public boolean getSeedEnabled() {
        return seedEnabled;
    }

    public void setSeedEnabled(boolean seedEnabled) {
        this.seedEnabled = seedEnabled;
    }

    public Resource getSeedLocation() {
        return seedLocation;
    }

    public void setSeedLocation(Resource seedLocation) {
        this.seedLocation = seedLocation;
    }

    public String getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(String defaultColor) {
        this.defaultColor = defaultColor;
    }

}
