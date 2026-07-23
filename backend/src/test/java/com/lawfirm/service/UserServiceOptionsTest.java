package com.lawfirm.service;

import com.lawfirm.dto.UserOptionDTO;
import com.lawfirm.entity.Department;
import com.lawfirm.entity.User;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceOptionsTest {

    private UserRepository userRepository;
    private DepartmentRepository departmentRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        service = new UserService(
                userRepository,
                departmentRepository,
                mock(RoleRepository.class),
                mock(UserRoleRepository.class),
                mock(PasswordEncoder.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void businessOptionsExposeOnlyMinimalActiveStaffIdentity() {
        User user = new User();
        user.setId(7L);
        user.setUsername("private-account");
        user.setRealName("测试律师");
        user.setPhone("13800000000");
        user.setEmail("private@example.com");
        user.setDepartmentId(3L);
        user.setPosition("律师");
        user.setStatus(1);
        user.setDeleted(false);

        Department department = new Department();
        department.setId(3L);
        department.setDeptName("民商法务部");

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(departmentRepository.findAllById(List.of(3L))).thenReturn(List.of(department));

        List<UserOptionDTO> result = service.getUserOptions(null, null, 500);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getId());
        assertEquals("测试律师", result.get(0).getRealName());
        assertEquals("民商法务部", result.get(0).getDepartmentName());
        assertEquals("律师", result.get(0).getPosition());

        Set<String> fields = Arrays.stream(UserOptionDTO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("id", "realName", "departmentId", "departmentName", "position"), fields);
        assertFalse(fields.contains("username"));
        assertFalse(fields.contains("phone"));
        assertFalse(fields.contains("email"));
        assertFalse(fields.contains("roles"));
    }
}
