// Conditional Docker agent image build — detects dependency/Dockerfile changes
// Skips rebuild when nothing changed, auto-rebuilds if image missing from host
//
// Usage: buildAgentImage()
//        buildAgentImage(baseTag: 'crm-agent', noCache: true)
def call(Map config = [:]) {

    // --- Configurable parameters with sensible defaults ---
    def baseDockerfile     = config.baseDockerfile     ?: 'cicd/Dockerfile'
    def prewarmedDockerfile = config.prewarmedDockerfile ?: 'cicd/Dockerfile-prewarmed'
    def baseTag            = config.baseTag            ?: 'flight-booking-agent'
    def imageTag           = config.imageTag           ?: 'flight-booking-agent-prewarmed'
    def rebuildTriggers    = config.rebuildTriggers    ?: ['pom.xml', 'cicd/Dockerfile', 'cicd/Dockerfile-prewarmed', 'cicd/settings.xml']
    def noCacheFlag        = config.noCache             ? '--no-cache' : ''

    echo "=== Build Agent Image: Evaluating rebuild necessity ==="

    def shouldRebuild = needsRebuild(rebuildTriggers, imageTag)

    if (shouldRebuild) {
        echo "Image rebuild triggered — building ${imageTag}..."

        // Step 1: Build base agent image
        echo "Step 1/3: Building base agent (${baseDockerfile})...${noCacheFlag ? ' (no-cache)' : ''}"
        sh "docker build ${noCacheFlag} -f ${baseDockerfile} -t ${baseTag}:latest ."

        // Step 2: Build prewarmed agent (extends base)
        echo "Step 2/3: Building prewarmed agent (${prewarmedDockerfile})...${noCacheFlag ? ' (no-cache)' : ''}"
        sh "docker build ${noCacheFlag} -f ${prewarmedDockerfile} -t ${imageTag}:latest ."

        // Step 3: Tag with build number for traceability
        def buildTag = "${imageTag}:build-${env.BUILD_NUMBER}"
        sh "docker tag ${imageTag}:latest ${buildTag}"
        echo "Step 3/3: Tagged ${buildTag}"

        echo "=== Build Agent Image: Complete (${imageTag}:latest + ${buildTag}) ==="
    } else {
        echo "=== Build Agent Image: Skipped (no rebuild-trigger changes detected) ==="
    }
}

// Returns true if image needs rebuilding (first build, image missing, or trigger files changed)
private boolean needsRebuild(List<String> rebuildTriggers, String imageTag) {

    // Check 1: First build — no image exists yet
    if (currentBuild.number == 1) {
        echo "Rebuild reason: First build — no previous image exists"
        return true
    }

    // Check 2: Image doesn't exist on this host (self-healing)
    // Handles: docker system prune, new Jenkins node, disk failure recovery
    def imageExists = sh(
        script: "docker image inspect ${imageTag}:latest > /dev/null 2>&1",
        returnStatus: true
    ) == 0

    if (!imageExists) {
        echo "Rebuild reason: Image '${imageTag}:latest' not found on host (pruned or new node)"
        return true
    }

    // Check 3: SCM changes — were any rebuild-trigger files modified?
    def changeLogSets = currentBuild.changeSets
    if (changeLogSets == null || changeLogSets.isEmpty()) {
        echo "No SCM changes detected — image is current"
        return false
    }

    for (changeSet in changeLogSets) {
        for (entry in changeSet.items) {
            for (file in entry.affectedFiles) {
                def filePath = file.path
                for (trigger in rebuildTriggers) {
                    if (filePath == trigger || filePath.endsWith("/${trigger}")) {
                        echo "Rebuild reason: ${filePath} matches trigger '${trigger}'"
                        return true
                    }
                }
            }
        }
    }

    echo "No rebuild-trigger files in changeset — image is current"
    return false
}
