const byId = (id) => document.getElementById(id);
const monthInput = byId("report-month");

let savingsChart = null;

// ----- Selected month: cards and category table -----

async function loadReport() {
    hideMessage();
    const month = monthInput.value;
    const query = month ? "?month=" + month : ""; // no month means "all time"

    try {
        const [summary, categories] = await Promise.all([
            apiRequest("/reports/summary" + query),
            apiRequest("/reports/categories" + query),
        ]);

        byId("report-income").textContent = formatMoney(summary.totalIncome);
        byId("report-expenses").textContent = formatMoney(summary.totalExpenses);
        byId("report-savings").textContent = formatMoney(summary.savings);
        applySavingsStyle(byId("report-savings"), summary.savings);

        renderCategoryTable(categories);
    } catch (error) {
        showMessage("error", describeError(error));
    }
}

function renderCategoryTable(categories) {
    const body = byId("category-report-body");
    body.replaceChildren();
    byId("category-report-wrap").classList.toggle("hidden", categories.length === 0);
    byId("category-report-empty").classList.toggle("hidden", categories.length > 0);

    for (const category of categories) {
        const row = document.createElement("tr");
        row.append(
            createElement("td", "", category.categoryName),
            createElement("td", "num", formatMoney(category.total)),
            createElement("td", "num", Number(category.percentage).toFixed(1) + "%")
        );
        body.append(row);
    }
}

// ----- All months: summary table and savings trend -----

async function loadTrends() {
    try {
        const monthly = await apiRequest("/reports/monthly");
        renderMonthlyTable(monthly);
        renderSavingsChart(monthly);
    } catch (error) {
        showMessage("error", describeError(error));
    }
}

function renderMonthlyTable(monthly) {
    const body = byId("monthly-report-body");
    body.replaceChildren();
    byId("monthly-report-wrap").classList.toggle("hidden", monthly.length === 0);
    byId("monthly-report-empty").classList.toggle("hidden", monthly.length > 0);

    // newest month first
    for (const entry of [...monthly].reverse()) {
        const row = document.createElement("tr");
        row.append(
            createElement("td", "", formatMonth(entry.month)),
            createElement("td", "num", formatMoney(entry.income)),
            createElement("td", "num", formatMoney(entry.expenses)),
            createElement("td", "num " + (entry.savings < 0 ? "expense" : "income"), formatMoney(entry.savings))
        );
        body.append(row);
    }
}

function renderSavingsChart(monthly) {
    const canvas = byId("savings-chart");
    const empty = byId("trend-empty");

    canvas.classList.toggle("hidden", monthly.length === 0);
    empty.classList.toggle("hidden", monthly.length > 0);
    if (monthly.length === 0) return;

    if (typeof Chart === "undefined") {
        canvas.classList.add("hidden");
        empty.textContent = "The chart library could not be loaded. Check your internet connection.";
        empty.classList.remove("hidden");
        return;
    }

    if (savingsChart) savingsChart.destroy();

    savingsChart = new Chart(canvas, {
        type: "line",
        data: {
            labels: monthly.map((entry) => formatMonth(entry.month)),
            datasets: [
                {
                    label: "Savings",
                    data: monthly.map((entry) => entry.savings),
                    borderColor: "#4f46e5",
                    backgroundColor: "rgba(79, 70, 229, 0.15)",
                    fill: true,
                    tension: 0.3,
                },
            ],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: (context) => "Savings: " + formatMoney(context.parsed.y),
                    },
                },
            },
            scales: {
                y: { ticks: { callback: (value) => formatCompact(value) } },
            },
        },
    });
}

// ----- Start -----

monthInput.addEventListener("change", loadReport);
monthInput.value = currentMonthString();
loadReport();
loadTrends();