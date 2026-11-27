// ================= VALIDATION & ROLE HELPERS =================
// Client-side validators and role sanitizers.
// Pure functions with zero side-effects.

export const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/;
export const NAME_REGEX = /^[A-Za-z\s.'-]+$/;

export const LIMITS = {
  NAME_MIN: 3,
  NAME_MAX: 100,
  EMAIL_MAX: 254,
  PASSWORD_MIN: 6,
  PASSWORD_MAX: 72, // BCrypt limit
};

export const OFFICIAL_ADMIN_EMAILS = new Set([
  "superadmin@gmail.com",
  "manideep@gmail.com",
  "sushil@gmail.com",
  "gaurav@gmail.com",
  "manideepsir@gmail.com",
  "sushilsir@gmail.com",
  "gauravsir@gmail.com",
]);

export const VALID_ROLES = new Set(["ADMIN", "MANAGER", "EMPLOYEE", "USER"]);

export const ROLE_PERMISSIONS = {
  ADMIN: [
    "EMPLOYEE_READ_ALL", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE", "EMPLOYEE_DELETE",
    "USER_MANAGE", "LEAVE_APPROVE_ALL",
    "EMPLOYEE_READ_ASSIGNED", "EMPLOYEE_UPDATE_ASSIGNED", "LEAVE_MANAGE_ASSIGNED",
    "LEAVE_APPLY", "LEAVE_READ_OWN", "REPORT_READ_OWN",
    "PROFILE_READ_OWN", "PROFILE_UPDATE_OWN",
  ],
  MANAGER: [
    "EMPLOYEE_READ_ASSIGNED", "EMPLOYEE_UPDATE_ASSIGNED", "LEAVE_MANAGE_ASSIGNED",
    "LEAVE_APPLY", "LEAVE_READ_OWN", "REPORT_READ_OWN",
    "PROFILE_READ_OWN", "PROFILE_UPDATE_OWN",
  ],
  EMPLOYEE: [
    "LEAVE_APPLY", "LEAVE_READ_OWN", "REPORT_READ_OWN",
    "PROFILE_READ_OWN", "PROFILE_UPDATE_OWN",
  ],
  USER: [
    "PROFILE_READ_OWN", "PROFILE_UPDATE_OWN",
  ],
};

export function normalizeUserRole(userObj) {
  if (!userObj) return userObj;
  const emailLower = userObj.email ? userObj.email.trim().toLowerCase() : "";
  if (emailLower === "superadmin@gmail.com") {
    return { ...userObj, name: "Super Admin", role: "ADMIN" };
  }
  if (emailLower && OFFICIAL_ADMIN_EMAILS.has(emailLower)) {
    return { ...userObj, name: userObj.name || "Admin", role: "ADMIN" };
  }
  const rawRole = userObj.role ? userObj.role.trim().toUpperCase().replace(/^ROLE_/, "") : "EMPLOYEE";
  const role = VALID_ROLES.has(rawRole) ? rawRole : "EMPLOYEE";
  return { ...userObj, name: userObj.name, role };
}

export function hasPermission(userObj, permission) {
  if (!userObj) return false;
  if (Array.isArray(userObj.permissions) && userObj.permissions.length > 0) {
    return userObj.permissions.includes(permission);
  }
  const normalized = normalizeUserRole(userObj);
  const perms = ROLE_PERMISSIONS[normalized?.role] || [];
  return perms.includes(permission);
}

export function getDefaultRouteForRole(userObj) {
  if (!userObj) return "/login";
  const role = normalizeUserRole(userObj)?.role;
  switch (role) {
    case "ADMIN":
      return "/admin";
    case "MANAGER":
      return "/manager";
    case "EMPLOYEE":
      return "/employee";
    case "USER":
      return "/profile";
    default:
      return "/profile";
  }
}

export function validateName(value) {
  const raw = value ?? "";
  const v = raw.trim();
  if (!v) return "Full name is required.";
  // REQUIREMENT: Name input must NOT allow numbers - only letters!
  if (/\d/.test(raw)) return "Numbers are not allowed in name. Please use only letters.";
  if (v.length < LIMITS.NAME_MIN) return `Name must be at least ${LIMITS.NAME_MIN} characters.`;
  if (v.length > LIMITS.NAME_MAX) return `Name must be at most ${LIMITS.NAME_MAX} characters.`;
  if (!NAME_REGEX.test(v)) return "Name can contain only letters, spaces, . ' and -.";
  return "";
}

export function validateEmail(value) {
  const v = (value ?? "").trim();
  if (!v) return "Email is required.";
  if (v.length > LIMITS.EMAIL_MAX) return "Email is too long.";
  if (!EMAIL_REGEX.test(v)) return "Enter a valid email address (e.g. you@example.com).";
  return "";
}

// mode: "login" -> only presence; "register" / "reset" -> full strength rules
export function validatePassword(value, mode = "register") {
  const v = value ?? "";
  if (!v) return "Password is required.";
  if (v.length > LIMITS.PASSWORD_MAX) return `Password must be at most ${LIMITS.PASSWORD_MAX} characters.`;
  if (mode === "login") return "";

  if (v.length < LIMITS.PASSWORD_MIN) return `Password must be at least ${LIMITS.PASSWORD_MIN} characters.`;
  if (/\s/.test(v)) return "Password must not contain spaces.";
  if (!/[a-z]/.test(v)) return "Password needs at least one lowercase letter.";
  if (!/[A-Z]/.test(v)) return "Password needs at least one uppercase letter.";
  if (!/\d/.test(v)) return "Password needs at least one number.";
  return "";
}

export function validateConfirmPassword(password, confirm) {
  if (!confirm) return "Please confirm your password.";
  if (password !== confirm) return "Passwords do not match.";
  return "";
}

// Returns an object of { field: errorMessage } for only the invalid fields.
export function validateAuthForm(values, isLogin, isForgotPassword = false) {
  const errors = {};
  const email = validateEmail(values.email);
  if (email) errors.email = email;

  if (isForgotPassword) {
    const password = validatePassword(values.password, "reset");
    if (password) errors.password = password;
    const confirm = validateConfirmPassword(values.password, values.confirmPassword);
    if (confirm) errors.confirmPassword = confirm;
    return errors;
  }

  const password = validatePassword(values.password, isLogin ? "login" : "register");
  if (password) errors.password = password;

  if (!isLogin) {
    const name = validateName(values.name);
    if (name) errors.name = name;
    const confirm = validateConfirmPassword(values.password, values.confirmPassword);
    if (confirm) errors.confirmPassword = confirm;
  }
  return errors;
}

// 0–4 score for a strength meter.
export function passwordStrength(value) {
  const v = value ?? "";
  let score = 0;
  if (v.length >= LIMITS.PASSWORD_MIN) score++;
  if (/[a-z]/.test(v) && /[A-Z]/.test(v)) score++;
  if (/\d/.test(v)) score++;
  if (/[^A-Za-z0-9]/.test(v)) score++;
  return score;
}

export function getInitials(name, email) {
  const emailLower = email ? email.trim().toLowerCase() : "";
  if (emailLower === "superadmin@gmail.com") return "SA";
  if (name) {
    const cleaned = name.replace(/[()]/g, "").trim();
    const parts = cleaned.split(/\s+/).filter(Boolean);
    if (parts.length >= 2) return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    return cleaned.slice(0, 2).toUpperCase();
  }
  return emailLower ? emailLower.slice(0, 2).toUpperCase() : "U";
}
