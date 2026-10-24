const API_BASE = "/api";

class ApiError extends Error {
    constructor(message, status, fieldErrors = {}) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

// ----- CSRF token -----

let csrfToken = null;
let csrfHeaderName = "X-CSRF-TOKEN";

async function loadCsrfToken() {
    let response;
    try {
        response = await fetch(API_BASE + "/auth/csrf");
    } catch (networkError) {
        throw new ApiError("Cannot reach the server. Is the application running?", 0);
    }
    const data = await response.json();
    csrfToken = data.token;
    csrfHeaderName = data.headerName;
}

function isAuthPage() {
    const path = window.location.pathname;
    return path.endsWith("/login.html") || path.endsWith("/register.html");
}

// ----- Talking to the backend -----

// Sends a request and returns the parsed JSON (or null for 204 No Content).
async function apiRequest(path, options = {}, isRetry = false) {
    const method = (options.method || "GET").toUpperCase();
    const headers = { "Content-Type": "application/json" };

    // Requests that change data must carry the CSRF token
    if (method !== "GET") {
        if (!csrfToken) await loadCsrfToken();
        headers[csrfHeaderName] = csrfToken;
    }

    let response;
    try {
        response = await fetch(API_BASE + path, { ...options, headers });
    } catch (networkError) {
        throw new ApiError("Cannot reach the server. Is the application running?", 0);
    }

    // A 403 on a changing request usually means our token is stale: fetch a new one and retry once
    if (response.status === 403 && method !== "GET" && !isRetry) {
        csrfToken = null;
        return apiRequest(path, options, true);
    }

    if (response.status === 204) {
        return null;
    }

    let data = null;
    try {
        data = await response.json();
    } catch (parseError) {
        // the response had no JSON body
    }

    // Not logged in (or the session expired): go to the login page
    if (response.status === 401 && !isAuthPage()) {
        window.location.href = "login.html";
    }

    if (!response.ok) {
        let message = "Something went wrong (status " + response.status + ")";
        if (data && data.message) {
            message = data.message;
        } else if (response.status === 401) {
            message = "Please log in to continue.";
        }
        throw new ApiError(message, response.status, (data && data.fieldErrors) || {});
    }

    return data;
}

// ----- Formatting -----

const moneyFormatter = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
});

function formatMoney(amount) {
    return moneyFormatter.format(amount);
}

// "2026-10-15" -> "15 Oct 2026" (built from the parts, so time zones cannot shift the day)
function formatDate(isoDate) {
    const [year, month, day] = isoDate.split("-").map(Number);
    return new Date(year, month - 1, day).toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}

// Today's date in yyyy-MM-dd using the user's local time
function todayString() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return now.getFullYear() + "-" + month + "-" + day;
}

// ----- Messages -----

let messageTimer = null;

function showMessage(type, text) {
    const box = document.getElementById("page-message");
    if (!box) return;
    box.textContent = text;
    box.className = "alert " + (type === "success" ? "alert-success" : "alert-error");
    clearTimeout(messageTimer);
    if (type === "success") {
        messageTimer = setTimeout(hideMessage, 4000);
    }
}

function hideMessage() {
    const box = document.getElementById("page-message");
    if (box) box.className = "alert hidden";
}

// Combines the main message with any per-field validation messages
function describeError(error) {
    const details = Object.values(error.fieldErrors || {});
    if (details.length === 0) return error.message;
    return error.message + ": " + details.join("; ");
}

// ----- DOM helper -----

// Builds an element using textContent, which prevents injected HTML (XSS)
function createElement(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

// ----- Month and chart helpers -----

// "2026-10" -> "Oct 2026" (or "October 2026" when style is "long")
function formatMonth(yearMonth, style = "short") {
    const [year, month] = yearMonth.split("-").map(Number);
    return new Date(year, month - 1, 1).toLocaleDateString("en-IN", {
        month: style,
        year: "numeric",
    });
}

// The current month as "yyyy-MM", using the user's local time
function currentMonthString() {
    const now = new Date();
    return now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0");
}

// 50000 -> "50K" (used on chart axes)
function formatCompact(amount) {
    return new Intl.NumberFormat("en-IN", {
        notation: "compact",
        maximumFractionDigits: 1,
    }).format(amount);
}

// Shows negative savings in red
function applySavingsStyle(element, value) {
    element.classList.toggle("expense", value < 0);
}

// ----- Logged-in user box in the sidebar -----

async function logout() {
    try {
        await apiRequest("/auth/logout", { method: "POST" });
    } catch (error) {
        // even if this fails, send the user to the login page
    }
    csrfToken = null;
    window.location.href = "login.html";
}

async function showUserBox() {
    const sidebar = document.querySelector(".sidebar");
    if (!sidebar) return; // login and register pages have no sidebar

    try {
        const user = await apiRequest("/auth/me");
        const box = createElement("div", "user-box");
        box.append(
            createElement("p", "user-name", user.displayName),
            createElement("p", "user-email", user.email)
        );
        const button = createElement("button", "btn btn-secondary btn-small", "Log out");
        button.type = "button";
        button.addEventListener("click", logout);
        box.append(button);
        sidebar.append(box);
    } catch (error) {
        // on a 401 apiRequest has already redirected to the login page
    }
}

showUserBox();