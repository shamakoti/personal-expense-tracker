const byId = (id) => document.getElementById(id);

let monthlyChart = null;

async function loadDashboard() {
    const month = currentMonthString();
    byId("current-month-label").textContent = "Overview for " + formatMonth(month, "long");

    try {
        const [allTime, thisMonth, recent, categories, monthly] = await Promise.all([
            apiRequest("/reports/summary"),
            apiRequest("/reports/summary?month=" + month),
            apiRequest("/transactions?size=5"),
            apiRequest("/reports/categories?month=" + month),
            apiRequest("/reports/monthly"),
        ]);

        renderSummaryCards(allTime, thisMonth);
        renderRecent(recent.content);
        renderCategories(categories);
        renderMonthlyChart(monthly);
    } catch (error) {
        showMessage("error", describeError(error));
    }
}

function renderSummaryCards(allTime, thisMonth) {
    byId("total-income").textContent = formatMoney(allTime.totalIncome);
    byId("total-expenses").textContent = formatMoney(allTime.totalExpenses);
    byId("total-savings").textContent = formatMoney(allTime.savings);
    applySavingsStyle(byId("total-savings"), allTime.savings);

    byId("month-income").textContent = formatMoney(thisMonth.totalIncome);
    byId("month-expenses").textContent = formatMoney(thisMonth.totalExpenses);
    byId("month-savings").textContent = formatMoney(thisMonth.savings);
    applySavingsStyle(byId("month-savings"), thisMonth.savings);
}

function renderRecent(transactions) {
    const body = byId("recent-body");
    body.replaceChildren();
    byId("recent-table-wrap").classList.toggle("hidden", transactions.length === 0);
    byId("recent-empty").classList.toggle("hidden", transactions.length > 0);

    for (const transaction of transactions) {
        const isIncome = transaction.type === "INCOME";
        const row = document.createElement("tr");
        row.append(
            createElement("td", "", formatDate(transaction.transactionDate)),
            createElement("td", "", transaction.description || "—"),
            createElement("td", "", transaction.categoryName),
            createElement(
                "td",
                "num " + (isIncome ? "income" : "expense"),
                (isIncome ? "+" : "−") + formatMoney(transaction.amount)
            )
        );
        body.append(row);
    }
}

function renderCategories(categories) {
    const list = byId("category-summary");
    list.replaceChildren();
    byId("category-empty").classList.toggle("hidden", categories.length > 0);

    for (const category of categories) {
        const percentage = Number(category.percentage).toFixed(1);

        const item = document.createElement("li");
        item.append(
            createElement("span", "", category.categoryName),
            createElement("span", "", formatMoney(category.total) + " · " + percentage + "%")
        );

        const track = createElement("div", "bar-track");
        track.setAttribute("aria-hidden", "true");
        const fill = createElement("div", "bar-fill");
        fill.style.width = percentage + "%";
        track.append(fill);
        item.append(track);

        list.append(item);
    }
}

function renderMonthlyChart(monthly) {
    const canvas = byId("monthly-chart");
    const empty = byId("chart-empty");
    const months = monthly.slice(-12); // the last 12 months with data

    canvas.classList.toggle("hidden", months.length === 0);
    empty.classList.toggle("hidden", months.length > 0);
    if (months.length === 0) return;

    if (typeof Chart === "undefined") {
        canvas.classList.add("hidden");
        empty.textContent = "The chart library could not be loaded. Check your internet connection.";
        empty.classList.remove("hidden");
        return;
    }

    if (monthlyChart) monthlyChart.destroy();

    monthlyChart = new Chart(canvas, {
        type: "bar",
        data: {
            labels: months.map((entry) => formatMonth(entry.month)),
            datasets: [
                { label: "Income", data: months.map((entry) => entry.income), backgroundColor: "#16a34a" },
                { label: "Expenses", data: months.map((entry) => entry.expenses), backgroundColor: "#dc2626" },
            ],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: (context) => context.dataset.label + ": " + formatMoney(context.parsed.y),
                    },
                },
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { callback: (value) => formatCompact(value) },
                },
            },
        },
    });
}

loadDashboard();