def call(String suiteName = '') {
    echo "================================================="
    echo "         BUILD & TEST METADATA"
    echo "================================================="
    echo "Job: ${env.JOB_NAME}, Build: ${env.BUILD_NUMBER}, Branch: ${env.BRANCH_NAME}"
    if (suiteName) {
        echo "Suite: ${suiteName}"
    }
    def causes = currentBuild.getBuildCauses()*.shortDescription.join(', ')
	echo "Triggered by: ${causes ?: 'Unknown'}"
    echo "================================================="
}
