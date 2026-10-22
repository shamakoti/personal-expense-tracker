const form = document.getElementById("transaction-form");
const idInput = document.getElementById("transaction-id");
const amountInput = document.getElementById("amount");
const typeSelect = document.getElementById("type");
const categorySelect = document.getElementById("category");
const dateInput = document.getElementById("date");
const descriptionInput = document.getElementById("description");
const submitButton = document.getElementById("submit-button");
const cancelButton = document.getElementById("cancel-edit");
const formTitle = document.getElementById("form-title");
const tableBody = document.getElementById("transactions-body");
const emptyState = document.getElementById("transactions-empty");
const deleteDialog = document.getElementById("delete-dialog");

let categories = [];
let transactions = [];
let pendingDeleteId = null;

// ----- Loading data -----

async function loadCategories() {
    try {
        categories = await apiRequest("/categories");
        fillCategoryOptions();
    } catch (error) {
        showMessage("error", describeError(error));
    }
}

async function loadTransactions() {
    try {
        transactions = await apiRequest("/transactions");
        renderTransactions();
    } catch (error) {
        showMessage("error", describeError(error));
    }
}

// Shows only the categories that match the selected type (income or expense)
function fillCategoryOptions(selectedId) {
    const matching = categories.filter((category) => category.type === typeSelect.value);
    categorySelect.replaceChildren();

    if (matching.length === 0) {
        categorySelect.append(new Option("No categories available", ""));
        return;
    }

    categorySelect.append(new Option("Select a category", ""));
    for (const category of matching) {
        categorySelect.append(new Option(category.name, category.id));
    }
    if (selectedId) {
        categorySelect.value = String(selectedId);
    }
}

// ----- Rendering the table -----

function renderTransactions() {
    tableBody.replaceChildren();
    emptyState.classList.toggle("hidden", transactions.length > 0);

    for (const transaction of transactions) {
        const row = document.createElement("tr");
        row.append(
            createElement("td", "", formatDate(transaction.transactionDate)),
            createElement("td", "", transaction.description || "—"),
            createElement("td", "", transaction.categoryName),
            buildTypeCell(transaction),
            buildAmountCell(transaction),
            buildActionsCell(transaction)
        );
        tableBody.append(row);
    }
}

function buildTypeCell(transaction) {
    const cell = document.createElement("td");
    const isIncome = transaction.type === "INCOME";
    cell.append(
        createElement(
            "span",
            "badge " + (isIncome ? "badge-income" : "badge-expense"),
            isIncome ? "Income" : "Expense"
        )
    );
    return cell;
}

function buildAmountCell(transaction) {
    const isIncome = transaction.type === "INCOME";
    const sign = isIncome ? "+" : "−";
    return createElement(
        "td",
        "num " + (isIncome ? "income" : "expense"),
        sign + formatMoney(transaction.amount)
    );
}

function buildActionsCell(transaction) {
    const cell = document.createElement("td");
    const wrapper = createElement("div", "row-actions");
    const label = transaction.description || transaction.categoryName;

    const editButton = createElement("button", "btn btn-small btn-secondary", "Edit");
    editButton.type = "button";
    editButton.setAttribute("aria-label", "Edit transaction: " + label);
    editButton.addEventListener("click", () => startEdit(transaction.id));

    const deleteButton = createElement("button", "btn btn-small btn-danger", "Delete");
    deleteButton.type = "button";
    deleteButton.setAttribute("aria-label", "Delete transaction: " + label);
    deleteButton.addEventListener("click", () => askDelete(transaction.id));

    wrapper.append(editButton, deleteButton);
    cell.append(wrapper);
    return cell;
}

// ----- Add and edit -----

function resetForm() {
    form.reset();
    idInput.value = "";
    dateInput.value = todayString();
    fillCategoryOptions();
    formTitle.textContent = "Add transaction";
    submitButton.textContent = "Save transaction";
    cancelButton.classList.add("hidden");
}

function startEdit(id) {
    const transaction = transactions.find((item) => item.id === id);
    if (!transaction) return;

    idInput.value = transaction.id;
    typeSelect.value = transaction.type;
    fillCategoryOptions(transaction.categoryId);
    amountInput.value = transaction.amount;
    dateInput.value = transaction.transactionDate;
    descriptionInput.value = transaction.description || "";

    formTitle.textContent = "Edit transaction";
    submitButton.textContent = "Update transaction";
    cancelButton.classList.remove("hidden");

    form.scrollIntoView({ behavior: "smooth" });
    amountInput.focus();
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    hideMessage();

    const payload = {
        amount: parseFloat(amountInput.value),
        type: typeSelect.value,
        categoryId: categorySelect.value ? Number(categorySelect.value) : null,
        transactionDate: dateInput.value,
        description: descriptionInput.value.trim() || null,
    };

    const editingId = idInput.value;
    const path = editingId ? "/transactions/" + editingId : "/transactions";
    const method = editingId ? "PUT" : "POST";

    submitButton.disabled = true;
    try {
        await apiRequest(path, { method, body: JSON.stringify(payload) });
        showMessage("success", editingId ? "Transaction updated." : "Transaction added.");
        resetForm();
        await loadTransactions();
    } catch (error) {
        showMessage("error", describeError(error));
    } finally {
        submitButton.disabled = false;
    }
});

typeSelect.addEventListener("change", () => fillCategoryOptions());
cancelButton.addEventListener("click", resetForm);

// ----- Delete with confirmation -----

function askDelete(id) {
    pendingDeleteId = id;
    deleteDialog.returnValue = "";
    deleteDialog.showModal();
}

deleteDialog.addEventListener("close", async () => {
    const id = pendingDeleteId;
    pendingDeleteId = null;
    if (deleteDialog.returnValue !== "confirm" || id === null) return;

    try {
        await apiRequest("/transactions/" + id, { method: "DELETE" });
        showMessage("success", "Transaction deleted.");
        if (idInput.value === String(id)) resetForm();
        await loadTransactions();
    } catch (error) {
        showMessage("error", describeError(error));
    }
});

// ----- Start -----

dateInput.value = todayString();
loadCategories();
loadTransactions();