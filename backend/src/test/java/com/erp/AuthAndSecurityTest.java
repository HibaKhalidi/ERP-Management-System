package com.erp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String user, String pass) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode n = json.readTree(r.getResponse().getContentAsString());
        return n.get("token").asText();
    }

    @Test
    void loginWithBadPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListProductsWithPagination() throws Exception {
        String token = login("admin", "admin123");
        mvc.perform(get("/api/products?size=2&q=ordinateur").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void viewerCannotCreateButSalesCannotManageUsers() throws Exception {
        String viewer = login("viewer", "viewer123");
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());

        String sales = login("sales", "sales123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + sales)).andExpect(status().isForbidden());
    }

    @Test
    void validationErrorsAreReturnedAsBadRequest() throws Exception {
        String token = login("admin", "admin123");
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void meReturnsRolesAndPermissions() throws Exception {
        String token = login("admin", "admin123");
        MvcResult r = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode n = json.readTree(r.getResponse().getContentAsString());
        assertThat(n.get("roles").toString()).contains("ADMIN");
        assertThat(n.get("permissions").toString()).contains("PRODUCT_WRITE");
    }
}
