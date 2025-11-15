package com.eugenezhu.voxforge.config;

import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.config
 * @className: OssConfig
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/24 下午3:19
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss.qiniu")
public class OssConfig {

    private String accessKey;
    private String secretKey;
    private String bucket;
    private String domain;
    private String region = "huadong"; // 默认华东区域

    @Bean
    public Auth qiniuAuth() {
        return Auth.create(accessKey, secretKey);
    }

    /**
     * 创建七牛云上传管理器
     */
    @Bean
    public UploadManager uploadManager() {
        Configuration cfg = new Configuration(getQiniuRegion());
        // 指定分片上传版本
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        return new UploadManager(cfg);
    }

    /**
     * 根据配置获取七牛云区域
     */
    private Region getQiniuRegion() {
        return switch (region.toLowerCase()) {
            case "huadong" -> Region.huadong();
            case "huabei" -> Region.huabei();
            case "huanan" -> Region.huanan();
            case "beimei" -> Region.beimei();
            case "xinjiapo" -> Region.xinjiapo();
            default -> Region.autoRegion(); // 自动选择区域
        };
    }
}

