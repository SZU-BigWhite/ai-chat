package com.aichat.config;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 知识树容量策略；递归实现的安全深度上限由固定协议常量约束。 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.tree")
public class TreeProperties {
    @Min(1)
    private int maxNodes = 3000;

    @Min(1)
    @Max(BusinessConstants.MAX_SUPPORTED_TREE_DEPTH)
    private int maxDepth = BusinessConstants.MAX_SUPPORTED_TREE_DEPTH;

    public int getMaxNodes() {
        return maxNodes;
    }

    public void setMaxNodes(int maxNodes) {
        this.maxNodes = maxNodes;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

}
