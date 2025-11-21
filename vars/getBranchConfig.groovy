def call() {
    def config = [
        // Active branches
        activeBranches: ['main','enhancements'],

        // Production candidate branches (notifications + deploy)
        productionCandidateBranches: ['main', 'enhancements'],

        // Development branches (full pipeline)
        developmentBranches: ['enhancements'],

        // Experimental branches (feature + improvement branches)
        experimentalBranches: ['feature/*', 'bugfix/*', 'improvements'],

        // AI analysis branches — production-candidates only
        // Feature branches skip AI analysis to save build time
        aiAnalysisBranches: ['main', 'enhancements']
    ]

    // Compute full-pipeline branch list (active + matching experimental)
    def branchName = env.BRANCH_NAME ?: 'unknown'
    config.pipelineBranches = config.activeBranches + config.experimentalBranches.findAll { pattern ->
        branchName.matches(pattern.replace('*', '.*'))
    }.collect { branchName } // Add current branch if it matches experimental patterns

    echo "Branch configuration loaded: ${config.activeBranches.size()} active branches"
    echo "Current branch '${branchName}' gets full pipeline: ${config.pipelineBranches.contains(branchName)}"
    return config
}