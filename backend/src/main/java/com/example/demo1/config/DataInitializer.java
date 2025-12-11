package com.example.demo1.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo1.common.enums.UserRole;
import com.example.demo1.entity.User;
import com.example.demo1.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds essential development data (like a demo user) so that the app
 * can be used immediately after resetting the database.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-user.username:demo}")
    private String demoUsername;

    @Value("${app.demo-user.password:123456}")
    private String demoPassword;

    @Override
    public void run(String... args) {
        long existing = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, demoUsername));
        if (existing > 0) {
            return;
        }

        User user = new User();
        user.setUsername(demoUsername);
        user.setEmail(demoUsername + "@example.com");
        user.setPassword(passwordEncoder.encode(demoPassword));
        user.setRole(UserRole.ADMIN);
        user.setBio("示例账号，方便开发调试。");
        user.setAvatarUrl(String.format("https://api.dicebear.com/7.x/thumbs/svg?seed=%s", demoUsername));

        userMapper.insert(user);
        log.info("Created default demo user '{}' for development login", demoUsername);
    }
}
