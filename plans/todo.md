# TODO List

## High Priority

### Investigate and Fix Streaming Test Root Cause
- **Issue**: DomainObjectIT.testStreamingCalculationsAccuracy consistently fails all retry attempts
- **Background**: Test fails with "Retry exhausted" even after adding exit code 1 handling
- **Investigation needed**:
  - Debug why ReactiveTransport fix didn't resolve streaming failures
  - Investigate resilience layer retry behavior in streaming context
  - Compare with Python SDK streaming implementation patterns
  - Determine if this is a fundamental CLI streaming issue
- **Impact**: Blocking CI success, affects streaming functionality confidence

### ClaudeSDKException Design Review
- **Issue**: ClaudeSDKException may not be needed at all
- **Background**: Changed from checked to runtime exception, but question remains if it's needed
- **Investigation needed**:
  - Can we use standard Java exceptions instead?
  - Are there Spring AI patterns we should follow?
  - What exceptions do other Spring AI models use?
- **Impact**: Simplifies error handling throughout codebase

## Medium Priority

### Add Docker Support to CI for Sandbox Infrastructure Tests
- **Issue**: Docker sandbox tests are skipped in CI (9 tests skipped)
- **Background**: Tests require `-Dsandbox.infrastructure.test=true` and Docker service
- **Implementation needed**:
  - Add Docker service to GitHub Actions workflow
  - Set system property in Maven command for CI
  - Ensure `ghcr.io/spring-ai-community/agents-runtime:latest` image availability
  - Configure Docker permissions and setup in CI environment
- **Impact**: Validates Docker sandbox functionality, increases test coverage by 9 tests

### Docker Claude CLI Path Resolution
- **Issue**: Docker tests can't find Claude CLI despite it being installed in host
- **Background**: Docker containers use different filesystem, claude CLI installed in custom Docker image
- **Investigation needed**:
  - Verify claude CLI location in agents-runtime Docker image
  - Check if ClaudeCliDiscovery handles Docker paths correctly
  - Test with correct Docker path
- **Impact**: Enables containerized testing, validates deployment scenarios

## Low Priority

### Optimize Test Performance and Timing Analysis
- **Issue**: Some integration tests take significant time, need performance insights
- **Background**: CI now collects detailed timing data via XML reports
- **Implementation needed**:
  - Download and analyze test timing reports from CI artifacts
  - Identify consistently slow tests (>30 seconds)
  - Optimize slow test queries or consider parallelization
  - Implement test categorization (fast/slow/integration)
- **Impact**: Faster CI feedback loops, better resource utilization

### Test Matrix Optimization (Future)
- **Issue**: Matrix CI was disabled due to Maven dependency resolution issues
- **Background**: Parallel test execution failed with "Could not find artifact" errors
- **Investigation for future**:
  - Re-enable matrix CI after stability improvements
  - Implement proper module dependency handling with `install` phase
  - Add conditional test execution based on file changes
  - Consider test splitting by provider (Claude/Gemini/SWE)
- **Impact**: Potentially 2-3x faster CI with parallel execution

## Recently Completed ✅

### CI Integration Tests and Timeout Fixes (Sept 28, 2025)
- ✅ **Integration Tests in CI**: Changed from `clean test` to `clean verify -Pfailsafe`
- ✅ **Timeout Resolution**: Increased all integration test timeouts to 3 minutes for CI environment
- ✅ **QuerySmokeTest Fixes**: Fixed all timeout issues in QuerySmokeTest methods
- ✅ **DomainObjectIT Fixes**: Added CLIOptions timeouts to all Query.execute() calls
- ✅ **Failsafe Rerun Config**: Added rerunFailingTestsCount=2 for flaky test resilience
- ✅ **Test Reporting**: XML reports with timing data uploaded as CI artifacts
- ✅ **Java Formatting**: Mandatory formatting requirements documented in CLAUDE.md
- ✅ **Streaming Fix Attempt**: Added exit code 1 handling for streaming operations (partial fix)

### Previous Completions
- ✅ Fixed unit tests by removing unnecessary checked exceptions
- ✅ Changed ClaudeSDKException from checked to runtime exception
- ✅ Made default permissions autonomous (BYPASS_PERMISSIONS)
- ✅ Added comprehensive debug logging to tests