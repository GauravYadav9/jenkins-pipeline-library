/**
 * Executes AI-powered failure analysis post-build.
 *
 * Parses Surefire XML for test failures, runs AiFailureAnalyzer via Maven,
 * and archives the generated analysis report as a build artifact.
 *
 * Advisory only — never blocks the pipeline. Gracefully skips if LLM
 * provider is unavailable, no tests ran, or no failures detected.
 *
 * Usage:
 *   analyzeFailuresWithAi()           // defaults from config.properties
 *   analyzeFailuresWithAi(skip: true) // skip AI analysis
 */
def call(Map config = [:]) {
    def skip = config.get('skip', false)

    if (skip) {
        echo "AI Failure Analysis: Skipped (disabled by parameter)"
        return
    }

    echo "AI Failure Analysis: Starting..."

    try {
        // Verify surefire XML exists (tests must have run)
        def xmlFiles = findFiles(glob: '**/surefire-reports/testng-results.xml')
        if (xmlFiles.size() == 0) {
            echo "AI Failure Analysis: No surefire XML found. Skipping."
            return
        }

        // Skip analysis if no failures detected
        def xmlContent = readFile(file: xmlFiles[0].path)
        def failedMatch = (xmlContent =~ /failed="(\d+)"/)
        def failedCount = failedMatch ? failedMatch[0][1].toInteger() : 0

        if (failedCount == 0) {
            echo "AI Failure Analysis: No failures detected. Skipping."
            return
        }

        echo "AI Failure Analysis: ${failedCount} failure(s) detected. Analyzing..."

        // Run AI failure analyzer via Maven exec
        // Uses test classpath since AiFailureAnalyzer is in src/test/java
		// AI base URL: defaults to Docker Desktop host resolution.
		// Override via AI_OLLAMA_BASE_URL env var for K8s/Linux environments.
		def aiBaseUrl = env.AI_OLLAMA_BASE_URL ?: 'http://host.docker.internal:11434'
        def exitCode = sh(
            script: """
                mvn exec:java \\
                    -Dexec.mainClass="com.demo.flightbooking.ai.FailureAnalysisRunner" \\
                    -Dexec.classpathScope=test \\
                    -Dai.ollama.baseUrl=${aiBaseUrl} \\
                    -q \\
                    2>&1 || true
            """,
            returnStatus: true
        )

        // Verify report generation
        def reportExists = fileExists('target/failure-analysis-report.md')

        if (reportExists) {
            echo "AI Failure Analysis: Report generated successfully."

            // Archive report as build artifact
            archiveArtifacts(
                artifacts: 'target/failure-analysis-report.md',
                allowEmptyArchive: true,
                fingerprint: false
            )

            echo "AI Failure Analysis: Report archived as build artifact."
        } else {
            echo "AI Failure Analysis: Report not generated (LLM may be unavailable). Pipeline continues."
        }

    } catch (Exception e) {
        // NEVER fail the build because of AI analysis
        echo "AI Failure Analysis: Skipped due to error: ${e.message}"
        echo "This does not affect test results or quality gate."
    }
}
