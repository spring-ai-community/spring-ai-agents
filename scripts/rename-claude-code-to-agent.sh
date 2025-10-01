#!/bin/bash
# Script to rename claude-code to claude-agent across the entire codebase
# This is a greenfield approach - no backward compatibility

set -e

PROJECT_ROOT="/home/mark/community/spring-ai-agents"
cd "$PROJECT_ROOT"

echo "Starting mass rename: claude-code → claude-agent"
echo "================================================"

# Step 1: Rename Maven modules
echo "[1/10] Renaming Maven modules..."
if [ -d "provider-sdks/claude-code-sdk" ]; then
    git mv provider-sdks/claude-code-sdk provider-sdks/claude-agent-sdk
    echo "  ✓ Renamed provider-sdks/claude-code-sdk → claude-agent-sdk"
fi

if [ -d "agent-models/spring-ai-claude-code" ]; then
    git mv agent-models/spring-ai-claude-code agent-models/spring-ai-claude-agent
    echo "  ✓ Renamed agent-models/spring-ai-claude-code → spring-ai-claude-agent"
fi

# Step 2: Rename Java package directories
echo "[2/10] Renaming Java package directories..."
cd "$PROJECT_ROOT"

# Rename in claude-agent-sdk
if [ -d "provider-sdks/claude-agent-sdk/src/main/java/org/springaicommunity/agents/claudecode" ]; then
    git mv provider-sdks/claude-agent-sdk/src/main/java/org/springaicommunity/agents/claudecode \
           provider-sdks/claude-agent-sdk/src/main/java/org/springaicommunity/agents/claude
    echo "  ✓ Renamed package: claudecode → claude (SDK main)"
fi

if [ -d "provider-sdks/claude-agent-sdk/src/test/java/org/springaicommunity/agents/claudecode" ]; then
    git mv provider-sdks/claude-agent-sdk/src/test/java/org/springaicommunity/agents/claudecode \
           provider-sdks/claude-agent-sdk/src/test/java/org/springaicommunity/agents/claude
    echo "  ✓ Renamed package: claudecode → claude (SDK test)"
fi

# Rename in spring-ai-claude-agent
if [ -d "agent-models/spring-ai-claude-agent/src/main/java/org/springaicommunity/agents/claudecode" ]; then
    git mv agent-models/spring-ai-claude-agent/src/main/java/org/springaicommunity/agents/claudecode \
           agent-models/spring-ai-claude-agent/src/main/java/org/springaicommunity/agents/claude
    echo "  ✓ Renamed package: claudecode → claude (Agent Model main)"
fi

if [ -d "agent-models/spring-ai-claude-agent/src/test/java/org/springaicommunity/agents/claudecode" ]; then
    git mv agent-models/spring-ai-claude-agent/src/test/java/org/springaicommunity/agents/claudecode \
           agent-models/spring-ai-claude-agent/src/test/java/org/springaicommunity/agents/claude
    echo "  ✓ Renamed package: claudecode → claude (Agent Model test)"
fi

# Step 3: Rename Java class files
echo "[3/10] Renaming Java class files..."
cd "$PROJECT_ROOT"

find . -type f -name "*.java" -path "*/org/springaicommunity/agents/claude/*" | while read file; do
    dir=$(dirname "$file")
    base=$(basename "$file")

    # Rename ClaudeCode* → ClaudeAgent*
    if [[ "$base" == ClaudeCode* ]]; then
        newname="${base/ClaudeCode/ClaudeAgent}"
        git mv "$file" "$dir/$newname"
        echo "  ✓ Renamed: $base → $newname"
    fi
done

# Step 4: Update pom.xml files
echo "[4/10] Updating pom.xml files..."
find . -name "pom.xml" -type f | while read pom; do
    # Update artifact IDs
    sed -i 's/claude-code-sdk/claude-agent-sdk/g' "$pom"
    sed -i 's/spring-ai-claude-code/spring-ai-claude-agent/g' "$pom"
    # Update names and descriptions
    sed -i 's/Claude Code SDK/Claude Agent SDK/g' "$pom"
    sed -i 's/Claude Code Agent/Claude Agent/g' "$pom"
    echo "  ✓ Updated: $pom"
done

# Step 5: Update Java source files (package declarations and imports)
echo "[5/10] Updating Java source files (packages and imports)..."
find . -name "*.java" -type f -not -path "*/target/*" | while read java; do
    # Update package declarations
    sed -i 's/package org\.springaicommunity\.agents\.claudecode/package org.springaicommunity.agents.claude/g' "$java"

    # Update imports
    sed -i 's/import org\.springaicommunity\.agents\.claudecode/import org.springaicommunity.agents.claude/g' "$java"

    # Update class name references in code
    sed -i 's/ClaudeCodeClient/ClaudeAgentClient/g' "$java"
    sed -i 's/ClaudeCodeAgentModel/ClaudeAgentModel/g' "$java"
    sed -i 's/ClaudeCodeAgentOptions/ClaudeAgentOptions/g' "$java"
    sed -i 's/ClaudeCodeAgentAutoConfiguration/ClaudeAgentAutoConfiguration/g' "$java"
    sed -i 's/ClaudeCodeAgentProperties/ClaudeAgentProperties/g' "$java"
done
echo "  ✓ Updated all Java source files"

# Step 6: Update Spring configuration files
echo "[6/10] Updating Spring configuration files..."
find . -name "spring.factories" -type f -not -path "*/target/*" | while read factories; do
    sed -i 's/ClaudeCodeAgentAutoConfiguration/ClaudeAgentAutoConfiguration/g' "$factories"
    sed -i 's/org\.springaicommunity\.agents\.claudecode/org.springaicommunity.agents.claude/g' "$factories"
    echo "  ✓ Updated: $factories"
done

# Step 7: Update application properties and YAML files
echo "[7/10] Updating application.properties and .yml files..."
find . \( -name "application.properties" -o -name "application.yml" -o -name "application-*.yml" -o -name "application-*.properties" \) -type f -not -path "*/target/*" | while read config; do
    sed -i 's/spring\.ai\.claude-code/spring.ai.claude-agent/g' "$config"
    sed -i 's/spring\.ai\.claudecode/spring.ai.claude/g' "$config"
    echo "  ✓ Updated: $config"
done

# Step 8: Update README and documentation
echo "[8/10] Updating README and documentation..."
find . \( -name "README.md" -o -name "*.md" \) -type f -not -path "*/target/*" -not -path "*/plans/*" | while read doc; do
    sed -i 's/claude-code-sdk/claude-agent-sdk/g' "$doc"
    sed -i 's/spring-ai-claude-code/spring-ai-claude-agent/g' "$doc"
    sed -i 's/Claude Code SDK/Claude Agent SDK/g' "$doc"
    sed -i 's/ClaudeCodeClient/ClaudeAgentClient/g' "$doc"
    sed -i 's/ClaudeCodeAgentModel/ClaudeAgentModel/g' "$doc"
    sed -i 's/ClaudeCodeAgentOptions/ClaudeAgentOptions/g' "$doc"
    sed -i 's/ClaudeCodeAgentAutoConfiguration/ClaudeAgentAutoConfiguration/g' "$doc"
    sed -i 's/ClaudeCodeAgentProperties/ClaudeAgentProperties/g' "$doc"
    echo "  ✓ Updated: $doc"
done

# Step 9: Update CLAUDE.md
echo "[9/10] Updating CLAUDE.md..."
if [ -f "CLAUDE.md" ]; then
    sed -i 's/claude-code-sdk/claude-agent-sdk/g' "CLAUDE.md"
    sed -i 's/spring-ai-claude-code/spring-ai-claude-agent/g' "CLAUDE.md"
    sed -i 's/Claude Code SDK/Claude Agent SDK/g' "CLAUDE.md"
    sed -i 's/ClaudeCodeClient/ClaudeAgentClient/g' "CLAUDE.md"
    sed -i 's/ClaudeCodeAgentModel/ClaudeAgentModel/g' "CLAUDE.md"
    echo "  ✓ Updated CLAUDE.md"
fi

# Step 10: Clean target directories
echo "[10/10] Cleaning target directories..."
./mvnw clean -q
echo "  ✓ Cleaned all target directories"

echo ""
echo "================================================"
echo "Mass rename complete!"
echo "Next steps:"
echo "1. Review changes: git status"
echo "2. Compile: ./mvnw clean compile"
echo "3. Run tests: ./mvnw clean test"
echo "4. Commit changes: git commit -am 'Rename claude-code to claude-agent'"
