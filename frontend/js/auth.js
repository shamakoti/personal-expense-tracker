const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");

if (loginForm) {
    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideMessage();

        const button = document.getElementById("login-button");
        button.disabled = true;
        try {
            await apiRequest("/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    email: document.getElementById("email").value,
                    password: document.getElementById("password").value,
                }),
            });
            window.location.href = "index.html";
        } catch (error) {
            showMessage("error", describeError(error));
            button.disabled = false;
        }
    });
}

if (registerForm) {
    registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        hideMessage();

        const password = document.getElementById("password").value;
        if (password !== document.getElementById("confirm-password").value) {
            showMessage("error", "The two passwords do not match.");
            return;
        }

        const button = document.getElementById("register-button");
        button.disabled = true;
        try {
            await apiRequest("/auth/register", {
                method: "POST",
                body: JSON.stringify({
                    displayName: document.getElementById("name").value,
                    email: document.getElementById("email").value,
                    password: password,
                }),
            });
            window.location.href = "index.html";
        } catch (error) {
            showMessage("error", describeError(error));
            button.disabled = false;
        }
    });
}