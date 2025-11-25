    def call(Map config) {
        if (!config.suiteName) {
            error "suiteName is required in sendBuildSummaryEmail.groovy"
        }

        def suiteName = config.suiteName
        def branchName = config.branchName ?: 'main'
        def summaryFile = "reports/${suiteName}-failure-summary.txt"
        def reportURL = "${env.BUILD_URL}Test_20Dashboard/".replace('%252F', '%2F')

        def failureSummary = fileExists(summaryFile) ? readFile(summaryFile).trim() : "⚠️ No failure summary available. Possible report generation issue."

        def subject, body
        if (currentBuild.currentResult == 'SUCCESS') {
            subject = "✅ SUCCESS: Build #${env.BUILD_NUMBER} for ${env.JOB_NAME}"
            body = """
                <p>Build was successful.</p>
                <p><b><a href='${reportURL}'>📄 View Test Dashboard</a></b></p>
            """
        } else if (currentBuild.currentResult == 'UNSTABLE') {
            subject = "⚠️ UNSTABLE: Build #${env.BUILD_NUMBER} for ${env.JOB_NAME}"
            body = """
                <p><b>WARNING: The build is unstable (some tests failed).</b></p>
                <p><b>Failure Summary:</b></p>
                <pre style="background-color:#F5F5F5; border:1px solid #E0E0E0; padding:10px; font-family:monospace;">${failureSummary}</pre>
                <p><b><a href='${reportURL}'>📊 View Test Dashboard</a></b></p>
            """
        } else {
            subject = "❌ FAILURE: Build #${env.BUILD_NUMBER} for ${env.JOB_NAME}"
            body = """
                <p><b>CRITICAL: The build has failed.</b></p>
                <p><b>Failure Summary:</b></p>
                <pre style="background-color:#F5F5F5; border:1px solid #E0E0E0; padding:10px; font-family:monospace;">${failureSummary}</pre>
                <p><b><a href='${reportURL}'>📊 View Test Dashboard</a></b></p>
            """
        }

        // Use different credentials based on branch
        def credentialId = (branchName in ['main', 'enhancements'])
            ? 'recipient-email-list' 
            : 'dev-recipient-email-list'

        withCredentials([string(credentialsId: credentialId, variable: 'RECIPIENT_EMAILS')]) {
            emailext(
                subject: subject,
                body: body,
                to: RECIPIENT_EMAILS,
                mimeType: 'text/html'
            )
        }
    }