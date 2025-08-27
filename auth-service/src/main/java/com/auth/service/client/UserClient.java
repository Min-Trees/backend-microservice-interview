package com.auth.service.client;

import com.auth.service.dto.request.UserSyncRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "user-service", url = "http://user-service")
public interface UserClient {
    @PostMapping("/users")
    void createUser(UserSyncRequest request);
}
