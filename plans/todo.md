# TODO List

## High Priority

### ClaudeSDKException Design Review
- **Issue**: ClaudeSDKException may not be needed at all
- **Background**: Changed from checked to runtime exception, but question remains if it's needed
- **Investigation needed**:
  - Can we use standard Java exceptions instead?
  - Are there Spring AI patterns we should follow?
  - What exceptions do other Spring AI models use?
- **Impact**: Simplifies error handling throughout codebase

## Medium Priority

### Docker Claude CLI Path Resolution
- **Issue**: Docker tests can't find Claude CLI despite it being installed in host
- **Background**: Docker containers use different filesystem, claude CLI installed in custom Docker image
- **Investigation needed**:
  - Verify claude CLI location in agents-runtime Docker image
  - Check if ClaudeCliDiscovery handles Docker paths correctly
  - Test with correct Docker path

## Completed
- ✅ Fixed unit tests by removing unnecessary checked exceptions
- ✅ Changed ClaudeSDKException from checked to runtime exception
- ✅ Made default permissions autonomous (BYPASS_PERMISSIONS)
- ✅ Added comprehensive debug logging to tests