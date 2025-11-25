def call(String suiteName, String buildNumber) {
    def summaryFile = "reports/${suiteName}-failure-summary.txt"
    def summaryExists = fileExists(summaryFile)
    def hasFailures = summaryExists && readFile(summaryFile).trim().toLowerCase().contains("failed")

    def reportChromeExists = fileExists("reports/chrome/index.html")
    def reportFirefoxExists = fileExists("reports/firefox/index.html")

    def failureSummary = "✅ All tests passed."
    def failureHeader = "✅ Test Result Summary"
    def failureBoxColor = "#f3fff3"
    def failureBorderColor = "#4CAF50"
    def failureTextColor = "#2e7d32"

    if (!reportChromeExists || !reportFirefoxExists) {
        hasFailures = true
        failureSummary = "❌ Reports not generated. Possible test or post-build failure."
        failureHeader = "⚠️ Report Generation Failed"
        failureBoxColor = "#fff3f3"
        failureBorderColor = "#f44336"
        failureTextColor = "#c62828"
    } else if (hasFailures) {
        failureSummary = readFile(summaryFile).trim()
        failureHeader = "❌ Failure Summary"
        failureBoxColor = "#fff3f3"
        failureBorderColor = "#f44336"
        failureTextColor = "#c62828"
    }

    def html = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>${suiteName.capitalize()} Test Dashboard</title>
            <style>
                body { font-family: Arial; padding: 20px; background-color: #f7f7f7; }
                h1 { color: #222; }
                ul { list-style-type: none; padding-left: 0; }
                li { margin: 10px 0; }
                a { color: #1976D2; font-size: 16px; text-decoration: none; }
                a:hover { text-decoration: underline; }
                .summary-box {
                    background-color: ${failureBoxColor};
                    border-left: 6px solid ${failureBorderColor};
                    padding: 10px;
                    margin-top: 20px;
                    white-space: pre-line;
                    font-family: monospace;
                    color: ${failureTextColor};
                }
            </style>
        </head>
        <body>
            <h1>📊 ${suiteName.capitalize()} Test Dashboard</h1>
            <ul>
                <li>🧪 <a href="chrome/index.html" target="_blank">Chrome Report</a></li>
                <li>🧪 <a href="firefox/index.html" target="_blank">Firefox Report</a></li>
            </ul>
            <p><strong>Build:</strong> #${buildNumber}</p>
            <h2>${failureHeader}</h2>
            <div class="summary-box">${failureSummary}</div>
        </body>
        </html>
    """

    writeFile file: 'reports/index.html', text: html
    echo "HTML dashboard generated for suite: ${suiteName}"
}