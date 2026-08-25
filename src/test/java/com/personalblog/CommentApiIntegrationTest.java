package com.personalblog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personalblog.post.Post;
import com.personalblog.post.PostRepository;
import com.personalblog.post.PostStatus;
import com.personalblog.user.BlogUser;
import com.personalblog.user.BlogUserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.jdbc.core.JdbcTemplate;
import com.personalblog.config.AuthRateLimitInterceptor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:reset-blog-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CommentApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BlogUserRepository users;
    @Autowired PostRepository posts;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthRateLimitInterceptor rateLimits;

    @BeforeEach void createPublishedPost() {
        rateLimits.clear();
        Instant now = Instant.now();
        posts.save(new Post("commented-post", "Commented post", "Summary", "Body", null, null,
            PostStatus.PUBLISHED, now, null, now, Set.of()));
    }

    @Test void commentsArePublicButCreationRequiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(get("/api/v1/posts/commented-post/comments"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(post("/api/v1/posts/commented-post/comments").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hello\"}"))
            .andExpect(status().isForbidden());

        Cookie session = createUserAndLogin("reader@example.com", true);
        mvc.perform(post("/api/v1/posts/commented-post/comments").cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hello\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void verifiedUserCanCreateAndDeleteOwnPlainTextComment() throws Exception {
        Cookie session = createUserAndLogin("reader@example.com", true);
        MvcResult created = mvc.perform(post("/api/v1/posts/commented-post/comments")
                .cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"  <script>alert('no')</script>  \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.authorDisplayName").value("Reader"))
            .andExpect(jsonPath("$.body").value("<script>alert('no')</script>"))
            .andExpect(jsonPath("$.canDelete").value(true)).andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/v1/posts/commented-post/comments"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].canDelete").value(false));
        mvc.perform(delete("/api/v1/comments/{id}", id).cookie(session).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/posts/commented-post/comments"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test void rejectsUnverifiedBlankOversizedAndNonOwnerActions() throws Exception {
        Cookie unverified = createUserAndLogin("unverified@example.com", false);
        mvc.perform(post("/api/v1/posts/commented-post/comments").cookie(unverified).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hello\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"));

        Cookie owner = createUserAndLogin("owner@example.com", true);
        mvc.perform(post("/api/v1/posts/commented-post/comments").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"   \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        String oversized = "x".repeat(2001);
        mvc.perform(post("/api/v1/posts/commented-post/comments").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + oversized + "\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        MvcResult created = mvc.perform(post("/api/v1/posts/commented-post/comments").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Owner comment\"}"))
            .andExpect(status().isCreated()).andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        Cookie other = createUserAndLogin("other@example.com", true);
        mvc.perform(delete("/api/v1/comments/{id}", id).cookie(other).with(csrf()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("COMMENT_DELETE_FORBIDDEN"));
        Cookie admin = createAdminAndLogin("admin@example.com");
        mvc.perform(delete("/api/v1/comments/{id}", id).cookie(admin).with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test void rateLimitsCommentCreationPerAccount() throws Exception {
        Cookie session = createUserAndLogin("frequent@example.com", true);
        for (int attempt = 1; attempt <= 10; attempt++) {
            mvc.perform(post("/api/v1/posts/commented-post/comments").cookie(session).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"body\":\"Comment " + attempt + "\"}"))
                .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/v1/posts/commented-post/comments").cookie(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"One too many\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("COMMENT_RATE_LIMIT"));
    }

    private Cookie createUserAndLogin(String email, boolean verified) throws Exception {
        BlogUser user = new BlogUser(email, passwords.encode("a-secure-password"),
            email.startsWith("reader") ? "Reader" : "Commenter", Instant.now());
        if (verified) user.verifyEmail(Instant.now());
        users.save(user);
        Cookie session = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"a-secure-password\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("BLOG_SESSION");
        if (session == null) throw new AssertionError("Login did not create BLOG_SESSION cookie");
        return session;
    }

    private Cookie createAdminAndLogin(String email) throws Exception {
        BlogUser user = new BlogUser(email, passwords.encode("a-secure-password"), "Admin", Instant.now());
        user.verifyEmail(Instant.now());
        users.saveAndFlush(user);
        jdbc.update("insert into user_roles (user_id, role) values (?, 'ADMIN')", user.getId());
        return login(email);
    }

    private Cookie login(String email) throws Exception {
        Cookie session = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"a-secure-password\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("BLOG_SESSION");
        if (session == null) throw new AssertionError("Login did not create BLOG_SESSION cookie");
        return session;
    }
}
