package com.xhulife.utils;

import com.xhulife.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AdminInterceptorTest {
    private final AdminInterceptor interceptor = new AdminInterceptor();
    @AfterEach void clear() { UserHolder.removeUser(); }
    @Test void anonymousIsUnauthorized() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, new Object()));
        assertEquals(401, response.getStatus());
    }
    @Test void normalUserIsForbidden() {
        UserDTO user = new UserDTO(); user.setId(1L); user.setRole("USER"); UserHolder.saveUser(user);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, new Object()));
        assertEquals(403, response.getStatus());
    }
    @Test void adminIsAllowed() {
        UserDTO user = new UserDTO(); user.setId(1L); user.setRole("ADMIN"); UserHolder.saveUser(user);
        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
    }
}
