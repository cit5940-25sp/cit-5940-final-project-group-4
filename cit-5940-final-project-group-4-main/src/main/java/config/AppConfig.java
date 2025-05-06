package config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 应用配置类
 */
@Slf4j
public class AppConfig {
    private static final Properties properties = new Properties();
    private static final AppConfig INSTANCE = new AppConfig();

    private AppConfig() {
        loadProperties();
    }

    /**
     * 获取配置实例
     */
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    /**
     * 加载配置文件
     */
    private void loadProperties() {
        try (InputStream input = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                log.error("无法找到 application.properties 文件");
                return;
            }
            properties.load(input);
        } catch (IOException e) {
            log.error("加载配置文件失败", e);
        }
    }

    /**
     * 获取配置项
     *
     * @param key 配置键
     * @return 配置值
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * 获取配置项，如果不存在则返回默认值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
