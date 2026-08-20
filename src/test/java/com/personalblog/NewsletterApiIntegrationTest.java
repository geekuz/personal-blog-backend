package com.personalblog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personalblog.user.BlogUser;
import com.personalblog.user.BlogUserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:reset-blog-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class NewsletterApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BlogUserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/newsletter/subscription"))
            .andExpect(status().isForbidden());
    }

    @Test void requiresVerifiedEmail() throws Exception {
        Cookie session = registerAndLogin();

        mvc.perform(post("/api/v1/newsletter/subscription").cookie(session).with(csrf()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"));
    }

    @Test void verifiedUserCanSubscribeAndUnsubscribeIdempotently() throws Exception {
        Cookie session = registerVerifyAndLogin();

        mvc.perform(get("/api/v1/newsletter/subscription").cookie(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.subscribed").value(false));
        mvc.perform(post("/api/v1/newsletter/subscription").cookie(session).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.subscribed").value(true));
        mvc.perform(post("/api/v1/newsletter/subscription").cookie(session).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.subscribed").value(true));
        mvc.perform(delete("/api/v1/newsletter/subscription").cookie(session).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/newsletter/subscription").cookie(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.subscribed").value(false));
    }

    private Cookie registerAndLogin() throws Exception {
        users.save(new BlogUser("reader@example.com", passwords.encode("a-secure-password"),
            "Reader One", Instant.now()));
        return login();
    }

    private Cookie registerVerifyAndLogin() throws Exception {
        BlogUser user = new BlogUser("reader@example.com", passwords.encode("a-secure-password"),
            "Reader One", Instant.now());
        user.verifyEmail(Instant.now());
        users.save(user);
        return login();
    }

    private Cookie login() throws Exception {
        Cookie session = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reader@example.com\",\"password\":\"a-secure-password\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("BLOG_SESSION");
        if (session == null) throw new AssertionError("Login did not create BLOG_SESSION cookie");
        return session;
    }
}
