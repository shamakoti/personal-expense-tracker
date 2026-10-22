const PAGE_SIZE = 10;

// ----- Form elements -----
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

// ----- Table and dialog elements -----
const tableBody = document.getElementById("transactions-body");
const emptyState = document.getElementById("transactions-empty");
const deleteDialog = document.getElementById("delete-dialog");

// ----- Filter and pagination elements -----
const filterForm = document.getElementById("filter-form");
const filterSearch = document.getElementById("filter-search");
const filterType = document.getElementById("filter-type");
const filterCategory = document.getElementById("filter-category");
const filterMonth = document.getElementById("filter-month");
const filterFrom = document.getElementById("filter-from");
const filterTo = document.getElementById("filter-to");
const filterSort = document.getElementById("filter-sort");
const prevButton = document.getElementById("prev-page");
const nextButton = document.getElementById("next-page");
const pageInfo = document.getElementById("page-info");

let categories = [];
let transactions = [];
let pendingDeleteId = null;
let currentPage = 0;
let totalPages = 1;

// ----- Loading data -----

async function loadCategories() {
    try {
        categories = await apiRequest("/categories");
        fillCategoryOptions();
        fillFilterCategories();
    } catch (error) {
        showMessage("error", describeError(error));
    }
}

// Turns the filter form into a query string such as "type=EXPENSE&month=2026-10&page=0"
function buildQueryString() {
    const params = new URLSearchParams();
    const add = (name, value) => {
        if (value) params.set(name, value);
    };

    add("search", filterSearch.value.trim());
    add("type", filterType.value);
    add("categoryId", filterCategory.value);
    add("month", filterMonth.value);
    add("from", filterFrom.value);
    add("to", filterTo.value);
    params.set("sort", filterSort.value);
    params.set("page", currentPage);
    params.set("size", PAGE_SIZE);
    return params.toString();
}

async function loadTransactions() {
    try {
        const result = await apiRequest("/transactions?" + buildQueryString());

        // If the current page no longer exists (e.g. the last item on it was deleted), step back
        if (currentPage > 0 && currentPage >= result.totalPages) {
            currentPage = Math.max(result.totalPages - 1, 0);
            return loadTransactions();
        }

        transactions = result.content;
        totalPages = Math.max(result.totalPages, 1);
        renderTransactions();
        updatePagination(result.totalElements);
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

function fillFilterCategories() {
    const selected = filterCategory.value;
    filterCategory.replaceChildren(new Option("All categories", ""));
    for (const category of categories) {
        const kind = category.type === "INCOME" ? "income" : "expense";
        filterCategory.append(new Option(category.name + " (" + kind + ")", category.id));
    }
    filterCategory.value = selected;
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

function updatePagination(totalElements) {
    const noun = totalElements === 1 ? " transaction" : " transactions";
    pageInfo.textContent =
        "Page " + (currentPage + 1) + " of " + totalPages + " · " + totalElements + noun;
    prevButton.disabled = currentPage === 0;
    nextButton.disabled = currentPage + 1 >= totalPages;
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

// ----- Search, filter, sort, paginate -----

filterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    currentPage = 0;
    loadTransactions();
});

// The reset event fires before the fields are cleared, so wait one tick
filterForm.addEventListener("reset", () => {
    setTimeout(() => {
        currentPage = 0;
        loadTransactions();
    }, 0);
});

// A month and a custom date range cannot be used together, so picking one clears the other
filterMonth.addEventListener("change", () => {
    if (filterMonth.value) {
        filterFrom.value = "";
        filterTo.value = "";
    }
});
filterFrom.addEventListener("change", () => {
    if (filterFrom.value) filterMonth.value = "";
});
filterTo.addEventListener("change", () => {
    if (filterTo.value) filterMonth.value = "";
});

prevButton.addEventListener("click", () => {
    if (currentPage > 0) {
        currentPage--;
        loadTransactions();
    }
});

nextButton.addEventListener("click", () => {
    if (currentPage + 1 < totalPages) {
        currentPage++;
        loadTransactions();
    }
});

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