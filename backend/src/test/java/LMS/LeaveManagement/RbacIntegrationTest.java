package LMS.LeaveManagement;

import LMS.LeaveManagement.entity.LeaveRequest;
import LMS.LeaveManagement.entity.Role;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.LeaveRequestRepository;
import LMS.LeaveManagement.repository.RoleRepository;
import LMS.LeaveManagement.repository.UserRepository;
import LMS.LeaveManagement.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:rbac_test_db;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
})
public class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String managerToken;
    private String employeeToken;
    private String otherEmployeeToken;
    private String userOnlyToken;

    private User managerUser;
    private User assignedEmployee;
    private User unassignedEmployee;
    private User userOnlyAccount;
    private LeaveRequest assignedLeave;
    private LeaveRequest unassignedLeave;

    @BeforeEach
    void setUpData() throws Exception {
        leaveRequestRepository.deleteAll();
        for (User u : userRepository.findAll()) {
            u.setManager(null);
            userRepository.save(u);
        }
        userRepository.deleteAll();

        Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN").orElse(null);
        Role managerRole = roleRepository.findByNameIgnoreCase("MANAGER").orElse(null);
        Role empRole = roleRepository.findByNameIgnoreCase("EMPLOYEE").orElse(null);
        Role userRole = roleRepository.findByNameIgnoreCase("USER").orElse(null);

        User admin = createUser("Super Admin", "superadmin@gmail.com", "ADMIN", adminRole, null);
        managerUser = createUser("Manager One", "mgr1@settribe.com", "MANAGER", managerRole, null);
        User otherManager = createUser("Manager Two", "mgr2@settribe.com", "MANAGER", managerRole, null);
        assignedEmployee = createUser("Assigned Emp", "emp1@settribe.com", "EMPLOYEE", empRole, managerUser);
        unassignedEmployee = createUser("Unassigned Emp", "emp2@settribe.com", "EMPLOYEE", empRole, otherManager);
        userOnlyAccount = createUser("Viewer User", "viewer@settribe.com", "USER", userRole, null);

        assignedLeave = new LeaveRequest();
        assignedLeave.setUser(assignedEmployee);
        assignedLeave.setLeaveType("CASUAL");
        assignedLeave.setFromDate(LocalDate.now().plusDays(2));
        assignedLeave.setToDate(LocalDate.now().plusDays(3));
        assignedLeave.setReason("Assigned leave test");
        assignedLeave.setStatus("PENDING");
        assignedLeave.setAssignedAdmin("Manager One (mgr1@settribe.com)");
        assignedLeave = leaveRequestRepository.save(assignedLeave);

        unassignedLeave = new LeaveRequest();
        unassignedLeave.setUser(unassignedEmployee);
        unassignedLeave.setLeaveType("SICK");
        unassignedLeave.setFromDate(LocalDate.now().plusDays(4));
        unassignedLeave.setToDate(LocalDate.now().plusDays(5));
        unassignedLeave.setReason("Unassigned leave test");
        unassignedLeave.setStatus("PENDING");
        unassignedLeave.setAssignedAdmin("Manager Two (mgr2@settribe.com)");
        unassignedLeave = leaveRequestRepository.save(unassignedLeave);

        adminToken = loginAndGetToken(admin.getEmail(), "Password@123");
        managerToken = loginAndGetToken(managerUser.getEmail(), "Password@123");
        employeeToken = loginAndGetToken(assignedEmployee.getEmail(), "Password@123");
        otherEmployeeToken = loginAndGetToken(unassignedEmployee.getEmail(), "Password@123");
        userOnlyToken = loginAndGetToken(userOnlyAccount.getEmail(), "Password@123");
    }

    private User createUser(String name, String email, String role, Role roleEntity, User manager) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("Password@123"));
        u.setRole(role);
        u.setAssignedRole(roleEntity);
        u.setManager(manager);
        u.setDepartment("Engineering");
        u.setDesignation(role);
        return userRepository.save(u);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    @Test
    @DisplayName("RBAC-01: Unauthenticated and expired JWT requests must return HTTP 401")
    void testUnauthenticatedAndExpiredJwtReturn401() throws Exception {
        mockMvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/leaves")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manager/employees")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/employees")).andExpect(status().isUnauthorized());

        // Expired token (-10 seconds)
        JwtService expiredJwtService = new JwtService(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", -10000L);
        var details = org.springframework.security.core.userdetails.User
                .withUsername(assignedEmployee.getEmail())
                .password("x")
                .roles("EMPLOYEE")
                .build();
        String expiredToken = expiredJwtService.generateToken(details);

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RBAC-02: USER role can access only own profile and is blocked (403) from leave, manager, and admin APIs")
    void testUserRolePermissions() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + userOnlyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("viewer@settribe.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(content().string(not(containsString("password"))));

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + userOnlyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex Updated\",\"phone\":\"9998887776\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex Updated"));

        // Cross-user profile access/modification blocked (403)
        mockMvc.perform(get("/api/profile/" + assignedEmployee.getId())
                        .header("Authorization", "Bearer " + userOnlyToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/profile/" + assignedEmployee.getId())
                        .header("Authorization", "Bearer " + userOnlyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked Name\"}"))
                .andExpect(status().isForbidden());

        // Blocked from employee leave, report, manager, and admin endpoints
        mockMvc.perform(get("/api/leaves").header("Authorization", "Bearer " + userOnlyToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/leaves/report").header("Authorization", "Bearer " + userOnlyToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/employees").header("Authorization", "Bearer " + userOnlyToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/employees").header("Authorization", "Bearer " + userOnlyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC-03: EMPLOYEE can apply/check own leaves & reports, cannot touch others' leaves or manager/admin APIs")
    void testEmployeeRolePermissions() throws Exception {
        String leaveJson = String.format(
                "{\"leaveType\":\"CASUAL\",\"fromDate\":\"%s\",\"toDate\":\"%s\",\"reason\":\"Personal day\"}",
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(11));

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/leaves/report")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeEmail").value("emp1@settribe.com"))
                .andExpect(jsonPath("$.totalRequests").value(2));

        // Attempt to update/delete another employee's leave -> 403
        mockMvc.perform(put("/api/leaves/" + unassignedLeave.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(leaveJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/leaves/" + unassignedLeave.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Blocked from manager and admin endpoints
        mockMvc.perform(get("/api/manager/employees").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/employees").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/leaves/" + assignedLeave.getId() + "/status")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC-04: MANAGER can view/edit only assigned employees and manage only assigned leaves")
    void testManagerRolePermissions() throws Exception {
        mockMvc.perform(get("/api/manager/employees")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("emp1@settribe.com"));

        // Edit permitted fields of assigned employee
        mockMvc.perform(put("/api/manager/employees/" + assignedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Assigned Emp Updated\",\"department\":\"Platform\",\"designation\":\"Senior Dev\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Platform"));

        // Attempt to escalate role via manager endpoint -> 403
        mockMvc.perform(put("/api/manager/employees/" + assignedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Assigned Emp\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        // Attempt to edit unassigned employee -> 403
        mockMvc.perform(put("/api/manager/employees/" + unassignedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized Edit\"}"))
                .andExpect(status().isForbidden());

        // View assigned leaves only
        mockMvc.perform(get("/api/manager/leaves")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(assignedLeave.getId()));

        // Approve assigned leave
        mockMvc.perform(put("/api/manager/leaves/" + assignedLeave.getId() + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Attempt to approve unassigned leave -> 403
        mockMvc.perform(put("/api/manager/leaves/" + unassignedLeave.getId() + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        // Blocked from ADMIN-only endpoints -> 403
        mockMvc.perform(get("/api/admin/employees").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/employees/" + assignedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC-05: ADMIN can create, read, update, and delete employee records and approve leaves without exposing passwords")
    void testAdminFullEmployeeCrudAndApprovals() throws Exception {
        // List all users without exposing password field
        mockMvc.perform(get("/api/admin/employees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("password"))));

        // Create new employee assigned to managerUser
        String createJson = String.format(
                "{\"name\":\"New Staff\",\"email\":\"newstaff@settribe.com\",\"password\":\"Password@123\",\"role\":\"EMPLOYEE\",\"managerId\":%d,\"department\":\"QA\",\"designation\":\"QA Analyst\"}",
                managerUser.getId());

        MvcResult createdRes = mockMvc.perform(post("/api/admin/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newstaff@settribe.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.managerId").value(managerUser.getId()))
                .andReturn();

        Long createdId = objectMapper.readTree(createdRes.getResponse().getContentAsString()).get("id").asLong();

        // Update employee role to USER
        mockMvc.perform(put("/api/admin/employees/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Staff Promoted\",\"role\":\"USER\",\"department\":\"Ops\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.department").value("Ops"));

        // Approve leave as ADMIN
        mockMvc.perform(put("/api/admin/leaves/" + unassignedLeave.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Delete created employee
        mockMvc.perform(delete("/api/admin/employees/" + createdId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId));

        assertThat(userRepository.findById(createdId)).isEmpty();
    }
}
