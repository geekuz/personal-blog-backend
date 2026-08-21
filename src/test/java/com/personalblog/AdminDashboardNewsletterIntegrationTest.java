package com.personalblog;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.personalblog.email.NewsletterEmailSender;
import com.personalblog.newsletter.NewsletterDeliveryService;
import com.personalblog.newsletter.NewsletterSubscription;
import com.personalblog.newsletter.NewsletterSubscriptionRepository;
import com.personalblog.post.Post;
import com.personalblog.user.BlogUser;
import com.personalblog.user.BlogUserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:reset-blog-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminDashboardNewsletterIntegrationTest {
    private static final String POST = """
        {"slug":"dashboard-post","title":"Dashboard post","summary":"A production-safe newsletter test.",
         "content":"# Hello","status":"PUBLISHED","tags":[{"name":"Java","slug":"java"}]}
        """;
    @Autowired MockMvc mvc;
    @Autowired BlogUserRepository users;
    @Autowired NewsletterSubscriptionRepository subscriptions;
    @Autowired NewsletterDeliveryService deliveries;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean NewsletterEmailSender sender;

    @Test void dashboardRequiresAdminRoleAndCsrfAndPublishingQueuesNewsletter() throws Exception {
        mvc.perform(get("/api/v1/dashboard")).andExpect(status().isForbidden());
        Cookie reader = createUserAndLogin("reader@example.com", false);
        mvc.perform(get("/api/v1/dashboard").cookie(reader)).andExpect(status().isForbidden());

        BlogUser subscriber = createUser("subscriber@example.com", true);
        subscriptions.save(new NewsletterSubscription(subscriber, Instant.now()));
        Cookie admin = createAdminAndLogin();

        mvc.perform(post("/api/v1/dashboard/posts").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content(POST))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/dashboard/posts").cookie(admin).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(POST))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PUBLISHED"));

        mvc.perform(get("/api/v1/dashboard").cookie(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publishedPosts").value(1))
            .andExpect(jsonPath("$.subscribers").value(1))
            .andExpect(jsonPath("$.pendingDeliveries").value(1))
            .andExpect(jsonPath("$.posts[0].slug").value("dashboard-post"));

        deliveries.dispatch();
        verify(sender).send(any(), eq("subscriber@example.com"), eq("Subscriber"), any(Post.class));
    }

    private BlogUser createUser(String email, boolean verified) {
        BlogUser user = new BlogUser(email, passwords.encode("a-secure-password"),
            email.startsWith("subscriber") ? "Subscriber" : "Reader", Instant.now());
        if (verified) user.verifyEmail(Instant.now());
        return users.saveAndFlush(user);
    }
    private Cookie createUserAndLogin(String email, boolean verified) throws Exception {
        createUser(email, verified); return login(email);
    }
    private Cookie createAdminAndLogin() throws Exception {
        BlogUser user = createUser("admin@example.com", true);
        jdbc.update("insert into user_roles (user_id, role) values (?, 'ADMIN')", user.getId());
        return login(user.getEmail());
    }
    private Cookie login(String email) throws Exception {
        Cookie session = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"a-secure-password\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("BLOG_SESSION");
        if (session == null) throw new AssertionError("Login did not create a session");
        return session;
    }
}
