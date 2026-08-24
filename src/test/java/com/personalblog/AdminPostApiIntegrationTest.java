package com.personalblog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:reset-blog-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminPostApiIntegrationTest {
    private static final String KEY = "test-admin-key";
    private static final String POST = """
        {
          "slug": "api-created-post",
          "title": "Created through the API",
          "summary": "A post managed through the protected admin endpoint.",
          "content": "# Hello\\n\\nThis content came from the API.",
          "coverImageUrl": "https://images.example.com/cover.jpg",
          "coverImageAlt": "Abstract API illustration",
          "status": "PUBLISHED",
          "tags": [{"name": "API", "slug": "api"}]
        }
        """;

    @Autowired MockMvc mvc;

    @Test void rejectsMissingAdminKey() throws Exception {
        mvc.perform(post("/api/v1/admin/posts").contentType(MediaType.APPLICATION_JSON).content(POST))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test void createsReadsUpdatesAndDeletesPost() throws Exception {
        mvc.perform(post("/api/v1/admin/posts").header("X-Admin-API-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(POST))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/admin/posts/api-created-post"))
            .andExpect(jsonPath("$.coverImageUrl").value("https://images.example.com/cover.jpg"))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mvc.perform(get("/api/v1/posts/api-created-post"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Created through the API"))
            .andExpect(jsonPath("$.coverImageAlt").value("Abstract API illustration"));

        String draft = POST.replace("\"status\": \"PUBLISHED\"", "\"status\": \"DRAFT\"");
        mvc.perform(put("/api/v1/admin/posts/api-created-post").header("X-Admin-API-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(draft))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.publishedAt").doesNotExist());

        mvc.perform(get("/api/v1/posts/api-created-post")).andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/admin/posts/api-created-post").header("X-Admin-API-Key", KEY))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/posts/api-created-post").header("X-Admin-API-Key", KEY))
            .andExpect(status().isNotFound());
    }

    @Test void rejectsCoverImageWithoutAltText() throws Exception {
        String invalid = POST.replace("\"coverImageAlt\": \"Abstract API illustration\",", "");
        mvc.perform(post("/api/v1/admin/posts").header("X-Admin-API-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(invalid))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
