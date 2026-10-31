package com.aichat.controller;

import com.aichat.model.dto.UpdateTopicDTO;
import com.aichat.model.dto.UpdateUserDTO;
import com.aichat.model.vo.AdminStatsVO;
import com.aichat.model.vo.TopicVO;
import com.aichat.model.vo.UserVO;
import com.aichat.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理接口仅做参数绑定与转发，权限由认证拦截器校验。 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/topics")
    public List<TopicVO> listTopics() {
        return adminService.listTopics();
    }

    @PostMapping("/topics")
    public TopicVO saveTopic(@Valid @RequestBody UpdateTopicDTO request) {
        return adminService.saveTopic(request);
    }

    @PutMapping("/topics/{id}")
    public TopicVO updateTopic(@PathVariable String id, @Valid @RequestBody UpdateTopicDTO request) {
        return adminService.updateTopic(id, request);
    }

    @GetMapping("/users")
    public List<UserVO> listUsers() {
        return adminService.listUsers();
    }

    @PatchMapping("/users/{id}")
    public UserVO updateUser(@PathVariable String id, @Valid @RequestBody UpdateUserDTO request) {
        return adminService.updateUser(id, request);
    }

    @GetMapping("/stats")
    public AdminStatsVO getStats() {
        return adminService.getStats();
    }
}
