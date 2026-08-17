package com.personalblog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:reset-blog-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthApiIntegrationTest {
    private static final String REGISTER = """
        {"email":"Reader@Example.com","password":"a-secure-password","displayName":"Reader One"}
        """;
    @Autowired MockMvc mvc;

    @Test void exposesCsrfTokenAndAnonymousSessionState() throws Exception {
        mvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
            .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false))
            .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test void registersNormalizesEmailAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("reader@example.com"))
            .andExpect(jsonPath("$.displayName").value("Reader One"))
            .andExpect(jsonPath("$.emailVerified").value(false))
            .andExpect(jsonPath("$.roles[0]").value("USER"));
        mvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test void loginPersistsAuthenticatedSessionAndLogoutInvalidatesIt() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(REGISTER)).andExpect(status().isCreated());

        MvcResult login = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reader@example.com\",\"password\":\"a-secure-password\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("reader@example.com"))
            .andReturn();
        Cookie session = login.getResponse().getCookie("BLOG_SESSION");
        if (session == null) throw new AssertionError("Login did not create BLOG_SESSION cookie");

        mvc.perform(get("/api/v1/auth/me").cookie(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.user.email").value("reader@example.com"));

        mvc.perform(post("/api/v1/auth/logout").cookie(session).with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test void rejectsInvalidCredentialsWithoutRevealingWhichFieldFailed() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
