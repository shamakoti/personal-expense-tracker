const categoryForm = document.getElementById("category-form");
const categoryNameInput = document.getElementById("category-name");
const categoryTypeSelect = document.getElementById("category-type");

async function loadCategories() {
    try {
        const categories = await apiRequest("/categories");
        renderList(
            "expense-categories",
            "expense-categories-empty",
            categories.filter((category) => category.type === "EXPENSE"),
            "No expense categories yet."
        );
        renderList(
            "income-categories",
            "income-categories-empty",
            categories.filter((category) => category.type === "INCOME"),
            "No income categories yet."
        );
    } catch (error) {
        showMessage("error", describeError(error));
    }
}

function renderList(listId, emptyId, items, emptyText) {
    const list = document.getElementById(listId);
    const empty = document.getElementById(emptyId);

    list.replaceChildren();
    empty.textContent = emptyText;
    empty.classList.toggle("hidden", items.length > 0);

    for (const category of items) {
        const item = document.createElement("li");
        item.append(createElement("span", "", category.name));
        item.append(
            category.defaultCategory
                ? createElement("span", "badge badge-default", "Default")
                : createElement("small", "", "Custom")
        );
        list.append(item);
    }
}

categoryForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    hideMessage();

    const payload = {
        name: categoryNameInput.value.trim(),
        type: categoryTypeSelect.value,
    };

    try {
        await apiRequest("/categories", { method: "POST", body: JSON.stringify(payload) });
        showMessage("success", "Category added.");
        categoryNameInput.value = "";
        await loadCategories();
    } catch (error) {
        showMessage("error", describeError(error));
    }
});

loadCategories();