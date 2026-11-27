import test from "node:test";
import assert from "node:assert/strict";
import {
  validateName,
  validateEmail,
  validatePassword,
  validateConfirmPassword,
  validateAuthForm,
  passwordStrength,
  normalizeUserRole,
  hasPermission,
  getDefaultRouteForRole,
  getInitials,
} from "./validationUtils.js";

test("validateName: should accept valid alphabetic names with spaces and hyphens", () => {
  assert.equal(validateName("John Doe"), "");
  assert.equal(validateName("Mary-Jane Watson"), "");
  assert.equal(validateName("O'Connor"), "");
});

test("validateName: should reject names containing numbers", () => {
  assert.match(validateName("John123"), /Numbers are not allowed in name/);
  assert.match(validateName("4lex"), /Numbers are not allowed in name/);
});

test("validateName: should enforce minimum and maximum length bounds", () => {
  assert.match(validateName("Jo"), /at least 3 characters/);
  assert.match(validateName("a".repeat(101)), /at most 100 characters/);
});

test("validateEmail: should accept valid email format", () => {
  assert.equal(validateEmail("user@example.com"), "");
  assert.equal(validateEmail("employee.name@settribe.org"), "");
});

test("validateEmail: should reject malformed email format", () => {
  assert.match(validateEmail("invalid-email"), /Enter a valid email address/);
  assert.match(validateEmail("user@"), /Enter a valid email address/);
  assert.match(validateEmail(""), /Email is required/);
});

test("validatePassword: login mode only checks presence", () => {
  assert.equal(validatePassword("any", "login"), "");
  assert.match(validatePassword("", "login"), /Password is required/);
});

test("validatePassword: register/reset mode enforces complexity", () => {
  assert.match(validatePassword("short", "register"), /at least 6 characters/);
  assert.match(validatePassword("alllowercase1", "register"), /at least one uppercase/);
  assert.match(validatePassword("ALLUPPERCASE1", "register"), /at least one lowercase/);
  assert.match(validatePassword("NoNumberPass", "register"), /at least one number/);
  assert.match(validatePassword("Pass word1", "register"), /must not contain spaces/);
  assert.equal(validatePassword("ValidPass@123", "register"), "");
});

test("validateConfirmPassword: must match password", () => {
  assert.equal(validateConfirmPassword("Password@123", "Password@123"), "");
  assert.match(validateConfirmPassword("Password@123", "Different@123"), /Passwords do not match/);
});

test("validateAuthForm: comprehensive validation for login & register", () => {
  const emptyLogin = validateAuthForm({ email: "", password: "" }, true);
  assert.ok(emptyLogin.email);
  assert.ok(emptyLogin.password);

  const validRegister = validateAuthForm(
    { name: "John Doe", email: "john@example.com", password: "Password@123", confirmPassword: "Password@123" },
    false
  );
  assert.equal(Object.keys(validRegister).length, 0);
});

test("passwordStrength: returns 0-4 score based on length, cases, digits, symbols", () => {
  assert.equal(passwordStrength(""), 0);
  assert.equal(passwordStrength("abc"), 0); // < 6 chars
  assert.equal(passwordStrength("abcdef"), 1); // >= 6 chars
  assert.equal(passwordStrength("Abcdef"), 2); // lower + upper
  assert.equal(passwordStrength("Abcdef1"), 3); // + digit
  assert.equal(passwordStrength("Abcdef1@"), 4); // + special char
});

test("normalizeUserRole: Super Admin enforces role 'ADMIN' and name 'Super Admin'", () => {
  const rawSuper = { email: "superadmin@gmail.com", name: "Shri (Admin)", role: "EMPLOYEE" };
  const normalized = normalizeUserRole(rawSuper);
  assert.equal(normalized.name, "Super Admin");
  assert.equal(normalized.role, "ADMIN");
});

test("normalizeUserRole: Official Admin enforces role 'ADMIN'", () => {
  const rawAdmin = { email: "manideep@gmail.com", name: "Manideep Sir", role: "EMPLOYEE" };
  const normalized = normalizeUserRole(rawAdmin);
  assert.equal(normalized.role, "ADMIN");
});

test("getInitials: returns correct initials and handles Super Admin specially", () => {
  assert.equal(getInitials("Super Admin", "superadmin@gmail.com"), "SA");
  assert.equal(getInitials("John Doe", "john@example.com"), "JD");
  assert.equal(getInitials("Alice", "alice@example.com"), "AL");
  assert.equal(getInitials("", "employee@example.com"), "EM");
});

test("RBAC helpers: supports ADMIN, MANAGER, EMPLOYEE, and USER roles & routes", () => {
  const admin = normalizeUserRole({ email: "a@settribe.com", role: "ADMIN" });
  const manager = normalizeUserRole({ email: "m@settribe.com", role: "MANAGER" });
  const employee = normalizeUserRole({ email: "e@settribe.com", role: "EMPLOYEE" });
  const user = normalizeUserRole({ email: "u@settribe.com", role: "USER" });

  assert.equal(admin.role, "ADMIN");
  assert.equal(manager.role, "MANAGER");
  assert.equal(employee.role, "EMPLOYEE");
  assert.equal(user.role, "USER");

  assert.equal(getDefaultRouteForRole(admin), "/admin");
  assert.equal(getDefaultRouteForRole(manager), "/manager");
  assert.equal(getDefaultRouteForRole(employee), "/employee");
  assert.equal(getDefaultRouteForRole(user), "/profile");

  assert.equal(hasPermission(admin, "EMPLOYEE_DELETE"), true);
  assert.equal(hasPermission(manager, "EMPLOYEE_DELETE"), false);
  assert.equal(hasPermission(manager, "LEAVE_MANAGE_ASSIGNED"), true);
  assert.equal(hasPermission(employee, "LEAVE_APPLY"), true);
  assert.equal(hasPermission(employee, "LEAVE_MANAGE_ASSIGNED"), false);
  assert.equal(hasPermission(user, "PROFILE_READ_OWN"), true);
  assert.equal(hasPermission(user, "LEAVE_APPLY"), false);
});
