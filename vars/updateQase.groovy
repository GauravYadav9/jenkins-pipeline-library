// vars/updateQase.groovy

def call(Map config) {
    try {
        echo '--- [Shared Library] Starting Qase.io Integration ---'

        withCredentials([string(credentialsId: config.credentialsId, variable: 'QASE_TOKEN')]) {
            echo 'Creating a new Test Run...'
            sh """
                curl -s -X POST "https://api.qase.io/v1/run/${config.projectCode}" \\
                -H "accept: application/json" \\
                -H "Content-Type: application/json" \\
                -H "Token: \$QASE_TOKEN" \\
                -d '{\"title\":\"${env.JOB_NAME} - Build ${env.BUILD_NUMBER}\", \"cases\":${config.testCaseIds}}' \\
                -o response.json
            """

            def responseJson = readJSON file: 'response.json'

            if (responseJson.status) {
                def runId = responseJson.result.id
                echo "Qase Test Run ID: ${runId}"

                echo "Uploading results to Qase..."
                sh """
                    curl -s -X PATCH "https://api.qase.io/v1/result/${config.projectCode}/${runId}/testng" \\
                    -H "accept: application/json" \\
                    -H "Content-Type: multipart/form-data" \\
                    -H "Token: \$QASE_TOKEN" \\
                    -F "file=@target/surefire-reports/testng-results.xml"
                """

                echo "Marking Qase run as complete..."
                sh """
                    curl -s -X POST "https://api.qase.io/v1/run/${config.projectCode}/${runId}/complete" \\
                    -H "accept: application/json" \\
                    -H "Token: \$QASE_TOKEN"
                """

                echo "Qase integration complete."
            } else {
                echo "Qase run creation failed: ${responseJson}"
            }
        }
    } catch (Exception err) {
        echo "Exception in Qase shared library: ${err.getMessage()}"
    }
}
