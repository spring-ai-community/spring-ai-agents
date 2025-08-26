# Spring AI PR #{pr_number} - Intelligent Conversation Analysis

You are analyzing a Spring AI pull request to provide deep insights into the problem, solution, and implementation quality. Use your expertise in software engineering, Spring Framework, and AI integration patterns.

## Context Data

### PR Information
- **Title**: {pr_title}
- **Author**: {pr_author}
- **State**: {pr_state}
- **Labels**: {pr_labels}

### PR Description
```
{pr_body}
```

### Linked Issues
{linked_issues_section}

### File Changes Summary
- **Total files changed**: {total_files_changed}
- **File types**: {file_types_summary}

### File Changes Detail
{file_changes_detail}

### Conversation Timeline
- **Total entries**: {total_conversation_entries}
- **Participants**: {total_participants}
- **Timeline**: {timeline_summary}

### Key Conversation Excerpts
{conversation_excerpts}

### Heuristic Analysis (Starting Point)
The pattern-matching analysis identified:
- **Problem Summary**: {heuristic_problem_summary}
- **Requirements Found**: {heuristic_requirements_count}
- **Concerns Detected**: {heuristic_concerns_count}
- **Themes**: {heuristic_themes}

## Analysis Request

**Special Note for Shallow PRs**: If this PR has minimal conversation or no linked issues, focus primarily on analyzing the file changes to understand the intent and implementation. Use the PR title and code changes to infer the problem being solved.

Please provide a comprehensive analysis with the following structure. Be concise but thorough, focusing on actionable insights:

### 1. PROBLEM_SUMMARY (2-3 sentences)
What specific problem is this PR solving? Focus on the core issue, not implementation details.

### 2. KEY_REQUIREMENTS (List of 5-8 items)
What are the essential functional and non-functional requirements? Consider:
- Core functionality needed
- Spring AI integration requirements
- Performance/compatibility constraints
- API design requirements

### 3. DESIGN_DECISIONS (List of 3-5 items)
What are the key architectural/design choices made? Include:
- Technology/library choices
- API design decisions
- Integration approaches
- Trade-offs made

### 4. OUTSTANDING_CONCERNS (List of genuine concerns)
What legitimate concerns or questions remain unresolved? Focus on:
- Technical risks or unknowns
- Compatibility/breaking change concerns
- Performance implications
- Security considerations

### 5. SOLUTION_APPROACHES (List of 2-4 approaches)
What solution approaches are discussed or implemented?
- Primary implementation approach
- Alternative approaches considered
- Implementation strategy

### 6. COMPLEXITY_INDICATORS (List complexity factors)
What factors indicate implementation complexity?
- Technical complexity factors
- Integration complexity
- Testing complexity
- Maintenance complexity

### 7. COMPLEXITY_SCORE (Integer 1-10)
Rate the overall complexity where:
- 1-3: Simple change, low risk
- 4-6: Moderate complexity, some risk
- 7-10: High complexity, significant risk

Consider: scope of changes, technical difficulty, integration points, testing needs, potential for breaking changes.

### 8. STAKEHOLDER_FEEDBACK (List of key feedback)
What are the main points of feedback from reviewers and stakeholders?
- Approval signals
- Concerns raised
- Suggestions made
- Review status

### 9. DISCUSSION_THEMES (List of main themes)
What are the primary discussion topics?
- Technical themes
- Process themes
- Quality themes

### 10. QUALITY_ASSESSMENT (2-3 sentences)
Overall assessment of the solution quality and approach.

### 11. RECOMMENDATIONS (List of 3-5 actionable recommendations)
What specific actions would improve this PR?
- Code quality improvements
- Testing recommendations
- Documentation needs
- Process improvements

## Response Format
Provide your analysis in JSON format with the following structure:

```json
{{
  "problem_summary": "...",
  "key_requirements": ["...", "..."],
  "design_decisions": ["...", "..."],
  "outstanding_concerns": ["...", "..."],
  "solution_approaches": ["...", "..."],
  "complexity_indicators": ["...", "..."],
  "complexity_score": 5,
  "stakeholder_feedback": ["...", "..."],
  "discussion_themes": ["...", "..."],
  "quality_assessment": "...",
  "recommendations": ["...", "..."]
}}
```

Focus on providing insights that would be valuable to a senior software engineer reviewing this PR for merge approval.