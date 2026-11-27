-- =======================================================
-- Database: settribe_leave_db (MySQL 8+)
-- Sample RBAC Roles, Permissions & Test Users
-- =======================================================

USE `settribe_leave_db`;

-- 1. Seed Roles
INSERT INTO `roles` (`name`, `description`) VALUES
('ADMIN', 'System ADMIN role with full employee/user CRUD and leave approvals'),
('MANAGER', 'System MANAGER role with assigned employee & leave management'),
('EMPLOYEE', 'System EMPLOYEE role with personal leave & report access'),
('USER', 'System USER role with profile-only access')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- 2. Seed Permissions
INSERT INTO `permissions` (`name`, `description`) VALUES
('EMPLOYEE_READ_ALL', 'View all employee and user records'),
('EMPLOYEE_CREATE', 'Create new employee, manager, or user records'),
('EMPLOYEE_UPDATE', 'Update employee records and assign roles or managers'),
('EMPLOYEE_DELETE', 'Delete employee or user records'),
('USER_MANAGE', 'Manage user accounts and role assignments'),
('LEAVE_APPROVE_ALL', 'Approve or reject leave applications'),
('EMPLOYEE_READ_ASSIGNED', 'View employees assigned to this manager'),
('EMPLOYEE_UPDATE_ASSIGNED', 'Edit permitted info for assigned employees'),
('LEAVE_MANAGE_ASSIGNED', 'Approve or reject assigned leave requests'),
('LEAVE_APPLY', 'Submit new leave applications'),
('LEAVE_READ_OWN', 'Check personal leave status and history'),
('REPORT_READ_OWN', 'View personal leave reports and summary'),
('PROFILE_READ_OWN', 'View personal profile information'),
('PROFILE_UPDATE_OWN', 'Update personal profile information')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- 3. Map Permissions to Roles
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r, `permissions` p WHERE r.`name` = 'ADMIN';

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
WHERE r.`name` = 'MANAGER'
  AND p.`name` IN ('EMPLOYEE_READ_ASSIGNED','EMPLOYEE_UPDATE_ASSIGNED','LEAVE_MANAGE_ASSIGNED','LEAVE_APPLY','LEAVE_READ_OWN','REPORT_READ_OWN','PROFILE_READ_OWN','PROFILE_UPDATE_OWN');

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
WHERE r.`name` = 'EMPLOYEE'
  AND p.`name` IN ('LEAVE_APPLY','LEAVE_READ_OWN','REPORT_READ_OWN','PROFILE_READ_OWN','PROFILE_UPDATE_OWN');

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id` FROM `roles` r JOIN `permissions` p
WHERE r.`name` = 'USER'
  AND p.`name` IN ('PROFILE_READ_OWN','PROFILE_UPDATE_OWN');

-- 4. Insert Users for all 4 RBAC Roles (Password: Password@123)
INSERT INTO `users` (`name`, `email`, `password`, `role`, `department`, `designation`) VALUES
('Super Admin', 'superadmin@gmail.com', '$2a$10$baN/1DaM95ALzvOHcy6FPeXLvk.sZXXHwciSnSNAaeOfi666iaGI6', 'ADMIN', 'Administration', 'Super Administrator'),
('Manideep Sir', 'manideep@gmail.com', '$2a$10$baN/1DaM95ALzvOHcy6FPeXLvk.sZXXHwciSnSNAaeOfi666iaGI6', 'ADMIN', 'Administration', 'Administrator'),
('Sushil Sir', 'sushil@gmail.com', '$2a$10$baN/1DaM95ALzvOHcy6FPeXLvk.sZXXHwciSnSNAaeOfi666iaGI6', 'ADMIN', 'Administration', 'Administrator'),
('Gaurav Sir', 'gaurav@gmail.com', '$2a$10$baN/1DaM95ALzvOHcy6FPeXLvk.sZXXHwciSnSNAaeOfi666iaGI6', 'ADMIN', 'Administration', 'Administrator'),
('Sarah Manager', 'manager@settribe.com', '$2a$10$baN/1DaM95ALzvOHcy6FPeXLvk.sZXXHwciSnSNAaeOfi666iaGI6', 'MANAGER', 'Engineering', 'Engineering Manager'),
('John Doe', 'john@settribe.com', '$2a$10$baN/1DaM95ALzvOHcy6FPeXLvk.sZXXHwciSnSNAaeOfi666iaGI6', 'EMPLOYEE', 'Engineering', 'Software Engineer'),
('Alex Viewer', 'user@settribe.com', '$2a$10$baN/1DaM95ALzvOHcy6FPeXLvk.sZXXHwciSnSNAaeOfi666iaGI6', 'USER', 'General', 'Profile Viewer')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `role` = VALUES(`role`);

-- Link role_id & manager_id relationships
UPDATE `users` u JOIN `roles` r ON UPPER(u.`role`) = r.`name` SET u.`role_id` = r.`id`;
UPDATE `users` emp JOIN `users` mgr ON mgr.`email` = 'manager@settribe.com'
SET emp.`manager_id` = mgr.`id` WHERE emp.`email` = 'john@settribe.com';

-- 5. Sample Leave Requests
INSERT INTO `leave_requests` (`user_id`, `leave_type`, `from_date`, `to_date`, `reason`, `status`, `assigned_admin`, `shared_with`)
SELECT `id`, 'CASUAL', '2026-12-02', '2026-12-04', 'Attending family wedding ceremony', 'PENDING', 'Manideep Sir (manideep@gmail.com)', 'Manideep Sir (manideep@gmail.com)'
FROM `users` WHERE `email` = 'john@settribe.com'
UNION ALL
SELECT `id`, 'SICK', '2026-11-15', '2026-11-16', 'Severe fever and doctor consultation', 'APPROVED', 'Sushil Sir (sushil@gmail.com)', 'Sushil Sir (sushil@gmail.com)'
FROM `users` WHERE `email` = 'john@settribe.com';
