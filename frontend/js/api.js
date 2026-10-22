const API_BASE = "/api";

class ApiError extends Error {
    constructor(message, status, fieldErrors = {}) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

// Sends a request to the backend and returns the parsed JSON (or null for 204 No Content).
async function apiRequest(path, options = {}) {
    let response;
    try {
        response = await fetch(API_BASE + path, {
            headers: { "Content-Type": "application/json" },
            ...options,
        });
    } catch (networkError) {
        throw new ApiError("Cannot reach the server. Is the application running?", 0);
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

    if (!response.ok) {
        const message = data && data.message
            ? data.message
            : "Something went wrong (status " + response.status + ")";
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