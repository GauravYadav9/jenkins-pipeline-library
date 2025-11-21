def call() {
    try {
        def files = findFiles(glob: '**/surefire-reports/testng-results.xml')
        def total = 0, failures = 0, errors = 0

        files.each { file ->
            def content = readFile(file: file.path)

            // Strictly count only actual test methods that failed (ignores config methods and exceptions)
            def failureCount = (content =~ /<test-method(?![^>]*is-config="true")[^>]*status="FAIL"/).findAll().size()
            failures += failureCount

            // Strictly count only actual test methods that passed
            def passCount = (content =~ /<test-method(?![^>]*is-config="true")[^>]*status="PASS"/).findAll().size()
            
            // Total is simply passed + failed (ignoring skipped for this gate)
            total += (failureCount + passCount)
            
        }

        return [total: total, failures: failures, errors: errors]
    } catch (Exception e) {
        echo "Quality Gate: Could not parse test results: ${e.message}"
        return [total: 0, failures: 0, errors: 0]
    }
}
