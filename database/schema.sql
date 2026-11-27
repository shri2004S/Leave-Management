-- =======================================================
-- Database: settribe_leave_db (MySQL 8+)
-- Employee Leave Management System - RBAC Schema Definition
-- =======================================================

CREATE DATABASE IF NOT EXISTS `settribe_leave_db`;
USE `settribe_leave_db`;

-- -------------------------------------------------------
-- Table 1: permissions
-- Fine-grained system permissions for RBAC enforcement
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `permissions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(80) NOT NULL UNIQUE,
    `description` VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- Table 2: roles
-- System roles: ADMIN, MANAGER, EMPLOYEE, USER
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `roles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(30) NOT NULL UNIQUE,
    `description` VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- Table 3: role_permissions
-- Many-to-Many mapping between roles and permissions
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `role_permissions` (
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`),
    CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- Table 4: users
-- Stores employees, managers, admins, and profile-only users
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
    `role_id` BIGINT NULL,
    `manager_id` BIGINT NULL,
    `department` VARCHAR(100) NULL,
    `designation` VARCHAR(100) NULL,
    `phone` VARCHAR(30) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_manager` FOREIGN KEY (`manager_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- Table 5: leave_requests
-- Stores employee leave requests with status, dates & approver
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `leave_requests` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `leave_type` VARCHAR(50) NOT NULL,
    `from_date` DATE NOT NULL,
    `to_date` DATE NOT NULL,
    `reason` VARCHAR(500) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `rejection_reason` VARCHAR(500) NULL,
    `assigned_admin` VARCHAR(150) NULL,
    `shared_with` VARCHAR(150) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_leave_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `users` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------
-- Performance Indexes
-- -------------------------------------------------------
CREATE INDEX `idx_user_role` ON `users` (`role`);
CREATE INDEX `idx_user_manager` ON `users` (`manager_id`);
CREATE INDEX `idx_leave_user_id` ON `leave_requests` (`user_id`);
CREATE INDEX `idx_leave_status` ON `leave_requests` (`status`);
