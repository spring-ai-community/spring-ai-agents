# Judge Framework Test Coverage Plan

## Overview
Comprehensive test strategy for the Judge API framework, covering unit tests, integration tests, and edge cases across all modules.

**Current Status**: 7 test files for 40 production classes (~17% coverage)
**Target**: 90%+ test coverage with focus on critical paths and edge cases

---

## Test Categories

### 1. Core Abstractions Tests

#### `JudgmentTest.java` ⚠️ MISSING
- [ ] Builder pattern validation
- [ ] Static factory methods (pass(), fail(), abstain(), error())
- [ ] Convenience methods (pass(), elapsed(), error())
- [ ] Metadata accessors
- [ ] Immutability validation (defensive copies)
- [ ] JudgmentStatus transitions

#### `JudgmentStatusTest.java` ⚠️ MISSING
- [ ] Enum values and ordering
- [ ] pass() convenience method logic
- [ ] Integration with Judgment builder

#### `JudgmentContextTest.java` ⚠️ MISSING
- [ ] Builder pattern validation
- [ ] Required fields validation
- [ ] Optional fields (workspace, executionTime, etc.)
- [ ] Immutability

#### `CheckTest.java` ⚠️ MISSING
- [ ] Pass/fail factory methods
- [ ] Metadata attachment
- [ ] toString() formatting

---

### 2. Score System Tests

#### `ScoreTest.java` ✅ EXISTS
- [ ] Enhance: BooleanScore edge cases
- [ ] Enhance: NumericalScore constructor guards (value out of range)
- [ ] Enhance: NumericalScore.normalized() edge case (max==min)
- [ ] Enhance: CategoricalScore value validation

#### `ScoresTest.java` ⚠️ MISSING (NEW in refactoring)
- [ ] toNormalized() with null score → 0.0
- [ ] toNormalized() with BooleanScore → 1.0/0.0
- [ ] toNormalized() with NumericalScore → normalized()
- [ ] toNormalized() with CategoricalScore → map lookup
- [ ] toNormalized() with unknown score type → exception
- [ ] Empty category map handling

---

### 3. Deterministic Judges Tests

#### `FileExistsJudgeTest.java` ✅ EXISTS
- [ ] Enhance: Test with JudgmentStatus (not just boolean)
- [ ] Enhance: Workspace resolution
- [ ] Enhance: Symlink handling
- [ ] Enhance: Permission denied scenarios

#### `FileContentJudgeTest.java` ✅ EXISTS
- [ ] Enhance: All match modes (EXACT, CONTAINS, REGEX)
- [ ] Enhance: File not found → FAIL status
- [ ] Enhance: Regex compilation errors
- [ ] Enhance: Large file handling
- [ ] Enhance: Binary file handling

#### `CommandJudgeTest.java` ✅ EXISTS
- [ ] Enhance: Various exit codes
- [ ] Enhance: Timeout scenarios
- [ ] Enhance: Output capture (stdout/stderr)
- [ ] Enhance: Execution time metadata
- [ ] Enhance: Working directory handling

#### `BuildSuccessJudgeTest.java` ✅ EXISTS
- [ ] Enhance: Maven build failures
- [ ] Enhance: Gradle build support
- [ ] Enhance: Custom build tools
- [ ] Enhance: Build log parsing

---

### 4. LLM Judges Tests

#### `LLMJudgeTest.java` ⚠️ MISSING
- [ ] Template method pattern
- [ ] Prompt construction
- [ ] Response parsing
- [ ] Error handling (LLM failures, timeouts)
- [ ] Retry logic

#### `CorrectnessJudgeTest.java` ✅ EXISTS
- [ ] Enhance: YES/NO extraction logic
- [ ] Enhance: Reasoning extraction
- [ ] Enhance: Malformed responses
- [ ] Enhance: Various goal/output combinations

---

### 5. Agent Judge Tests

#### `AgentJudgeTest.java` ⚠️ MISSING (NEW in Phase 5)
- [ ] Pattern extraction (PASS, SCORE, REASONING)
- [ ] AgentClient integration
- [ ] Working directory context
- [ ] Goal template formatting
- [ ] Pre-configured factory methods (codeReview(), securityAudit())
- [ ] Agent execution failures

---

### 6. Voting Strategies Tests

#### `MajorityVotingStrategyTest.java` ⚠️ MISSING
- [ ] Simple majority (pass > fail)
- [ ] Tie with TiePolicy.PASS → PASS
- [ ] Tie with TiePolicy.FAIL → FAIL
- [ ] Tie with TiePolicy.ABSTAIN → ABSTAIN
- [ ] All ERROR + ErrorPolicy.TREAT_AS_FAIL → FAIL
- [ ] All ERROR + ErrorPolicy.TREAT_AS_ABSTAIN → ABSTAIN
- [ ] All ERROR + ErrorPolicy.IGNORE → ABSTAIN (all ignored)
- [ ] All ABSTAIN → ABSTAIN
- [ ] Mixed PASS/FAIL/ABSTAIN/ERROR
- [ ] Empty judgment list → exception
- [ ] getName() returns "majority"

#### `AverageVotingStrategyTest.java` ⚠️ MISSING
- [ ] Average calculation
- [ ] Threshold-based pass/fail
- [ ] Numerical score conversion
- [ ] Boolean score conversion
- [ ] getName() returns "average"

#### `WeightedAverageStrategyTest.java` ⚠️ MISSING
- [ ] Weighted average calculation
- [ ] Weight normalization
- [ ] Zero weights handling
- [ ] getName() returns "weighted"

#### `MedianVotingStrategyTest.java` ⚠️ MISSING
- [ ] Median calculation (odd count)
- [ ] Median calculation (even count - average of middle two)
- [ ] Outlier robustness
- [ ] getName() returns "median"

#### `ConsensusStrategyTest.java` ⚠️ MISSING
- [ ] Unanimous pass → PASS
- [ ] Unanimous fail → FAIL
- [ ] Mixed votes → FAIL (no consensus)
- [ ] getName() returns "consensus"

---

### 7. Jury System Tests

#### `SimpleJuryTest.java` ⚠️ MISSING
- [ ] Parallel execution (CompletableFuture)
- [ ] Sequential execution
- [ ] Judge name extraction (with/without metadata)
- [ ] Identity preservation (individualByName map)
- [ ] Verdict construction with builder
- [ ] Empty jury → exception in builder
- [ ] Custom executor support
- [ ] Voting strategy integration

#### `VerdictTest.java` ⚠️ MISSING (NEW in Phase 4)
- [ ] Builder pattern validation
- [ ] Immutability (defensive copies)
- [ ] individualByName map population
- [ ] subVerdicts for MetaJury
- [ ] Null handling in constructor

#### `JuriesTest.java` ⚠️ MISSING (NEW in Phase 6)
- [ ] fromJudges() with auto-naming
- [ ] fromJudges() with duplicate names → suffixes (-2, -3)
- [ ] fromJudges() with already-named judges
- [ ] combine() two juries
- [ ] allOf() multiple juries
- [ ] Empty judges array → exception

#### `MetaJuryTest.java` ⚠️ MISSING (NEW in Phase 6)
- [ ] Sub-jury execution
- [ ] Aggregation of jury verdicts
- [ ] Sub-verdicts preservation
- [ ] Empty juries → exception
- [ ] Null strategy → exception
- [ ] getJudges() returns empty list

---

### 8. Judge Composition Tests

#### `JudgesTest.java` ⚠️ MISSING
- [ ] named() wrapper
- [ ] tryMetadata() with JudgeWithMetadata
- [ ] tryMetadata() with plain Judge → Optional.empty()
- [ ] Composition utilities (if any)

#### `NamedJudgeTest.java` ⚠️ MISSING
- [ ] Metadata delegation
- [ ] Judge delegation
- [ ] toString() formatting

#### `AsyncJudgeTest.java` ⚠️ MISSING
- [ ] Async execution
- [ ] Timeout handling
- [ ] Exception propagation
- [ ] Thread pool management

#### `ReactiveJudgeTest.java` ⚠️ MISSING
- [ ] Reactive stream handling
- [ ] Backpressure
- [ ] Error handling

---

### 9. Integration Tests

#### `JudgeAdvisorTest.java` ⚠️ MISSING
- [ ] AgentClient integration
- [ ] Judgment context construction
- [ ] Response context attachment
- [ ] Timing capture (startTime, endTime, executionTime)
- [ ] Status determination (SUCCESS/FAILED)
- [ ] Builder pattern
- [ ] getName() formatting

#### `JuryAdvisorTest.java` ⚠️ MISSING (NEW in Phase 5)
- [ ] AgentClient integration
- [ ] Verdict context construction
- [ ] Response context attachment (verdict, verdict.aggregated, verdict.pass, verdict.status)
- [ ] Voting strategy name in getName()
- [ ] Builder pattern

#### `AgentClientResponseTest.java` ⚠️ MISSING
- [ ] getJudgment() accessor
- [ ] isJudgmentPassed() convenience method
- [ ] getVerdict() accessor (NEW)
- [ ] isVerdictPassed() convenience method (NEW)

---

### 10. Functional/End-to-End Tests

#### `JudgeFunctionalTest.java` ✅ EXISTS
- [ ] Enhance: Multi-judge scenarios
- [ ] Enhance: Jury composition
- [ ] Enhance: Meta-jury examples
- [ ] Enhance: Full advisor chain

#### `JuryWorkflowTest.java` ⚠️ MISSING
- [ ] File judge + build judge + correctness judge → weighted average
- [ ] Meta-jury: (jury1 + jury2) → consensus
- [ ] Error recovery scenarios
- [ ] Real AgentClient integration

---

## Test Utilities

### Test Fixtures (`JudgeTestFixtures.java`) ⚠️ MISSING
```java
public class JudgeTestFixtures {
    // Sample judges
    public static Judge alwaysPass(String name);
    public static Judge alwaysFail(String name);
    public static Judge alwaysAbstain(String name);
    public static Judge alwaysError(String name);

    // Sample contexts
    public static JudgmentContext simpleContext(String goal);
    public static JudgmentContext withWorkspace(String goal, Path workspace);

    // Sample judgments
    public static Judgment passJudgment(double score);
    public static Judgment failJudgment(double score);

    // Sample verdicts
    public static Verdict unanimousPass(int judgeCount);
    public static Verdict split(int passCount, int failCount);
}
```

### Mock Judges
- `MockJudge` - Configurable judge for testing
- `RecordingJudge` - Records all invocations for verification
- `SlowJudge` - Introduces delays for timeout testing

---

## Priority Matrix

### P0 (Critical - Must Have)
1. **Core abstractions**: Judgment, JudgmentContext, JudgmentStatus
2. **Voting strategies**: All 5 strategies with edge cases
3. **SimpleJury**: Parallel/sequential execution, identity preservation
4. **Juries utility**: fromJudges(), duplicate name handling
5. **Score conversion**: Scores.toNormalized() with all types

### P1 (High Priority)
1. **Deterministic judges**: File, command, build judges
2. **Verdict**: Builder, immutability, sub-verdicts
3. **MetaJury**: Sub-jury aggregation
4. **Judge composition**: Named, async, reactive

### P2 (Medium Priority)
1. **LLM judges**: Correctness, template patterns
2. **Agent judge**: Pattern extraction, integration
3. **Advisors**: JudgeAdvisor, JuryAdvisor integration
4. **AgentClientResponse**: Accessor methods

### P3 (Nice to Have)
1. **Performance tests**: Large juries, parallel execution benchmarks
2. **Stress tests**: Memory leaks, thread safety
3. **Documentation tests**: Example code in Javadocs

---

## Coverage Goals

| Category | Current | Target | Gap |
|----------|---------|--------|-----|
| Core Abstractions | 0% | 95% | 🔴 |
| Score System | ~30% | 90% | 🟡 |
| Deterministic Judges | ~60% | 85% | 🟡 |
| LLM Judges | ~40% | 80% | 🟡 |
| Agent Judge | 0% | 85% | 🔴 |
| Voting Strategies | 0% | 95% | 🔴 |
| Jury System | 0% | 95% | 🔴 |
| Composition | 0% | 80% | 🔴 |
| Integration | 0% | 75% | 🔴 |
| **Overall** | ~17% | **90%** | 🔴 |

---

## Test Implementation Order

### Phase 1: Foundation (P0)
1. JudgmentTest, JudgmentStatusTest, JudgmentContextTest
2. ScoresTest (new utility)
3. All 5 VotingStrategy tests with full edge case matrix

### Phase 2: Jury Core (P0)
1. SimpleJuryTest (parallel, sequential, identity)
2. VerdictTest (builder, immutability)
3. JuriesTest (fromJudges, duplicate handling)
4. MetaJuryTest (sub-jury aggregation)

### Phase 3: Judges Enhancement (P1)
1. Enhance existing deterministic judge tests
2. LLMJudgeTest, enhance CorrectnessJudgeTest
3. AgentJudgeTest (new)

### Phase 4: Integration (P1)
1. JudgeAdvisorTest
2. JuryAdvisorTest
3. AgentClientResponseTest

### Phase 5: Composition & Advanced (P2)
1. JudgesTest, NamedJudgeTest
2. AsyncJudgeTest, ReactiveJudgeTest
3. End-to-end workflow tests

---

## Continuous Testing Strategy

### During Development
- [ ] Write tests alongside production code (TDD when possible)
- [ ] Run `./mvnw test -pl agent-models/spring-ai-agents-judge` frequently
- [ ] Maintain >80% coverage for new code

### CI/CD Integration
- [ ] Run full test suite on every PR
- [ ] Generate coverage reports with JaCoCo
- [ ] Fail build if coverage drops below 85%

### Test Maintenance
- [ ] Review and update tests when refactoring
- [ ] Add tests for every bug fix
- [ ] Quarterly test review for obsolete tests

---

## Next Steps

1. **Create test fixtures** - `JudgeTestFixtures.java` with reusable mocks
2. **Implement P0 tests** - Focus on voting strategies and jury core
3. **Run coverage analysis** - `./mvnw verify jacoco:report`
4. **Fill gaps iteratively** - Follow implementation order above
5. **Document edge cases** - Keep this plan updated as tests are added
