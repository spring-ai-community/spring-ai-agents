# Judge Framework Test Coverage Status

## Current Coverage (as of Phases 1-6)

| Metric | Coverage | Target | Status |
|--------|----------|--------|--------|
| **Line Coverage** | 39.3% (257/654) | 90% | 🟡 |
| **Branch Coverage** | 25.7% (69/269) | 85% | 🟡 |
| **Instruction Coverage** | 41.1% (1343/3267) | 90% | 🟡 |

**Note**: Initial estimate of 17% was based on file count. Actual coverage is significantly higher due to existing comprehensive tests for deterministic judges.

## Coverage by Package

### Well-Covered (>60%)
- ✅ `org.springaicommunity.agents.judge.exec` - Command and build judges (~75%)
- ✅ `org.springaicommunity.agents.judge.fs` - File system judges (~70%)
- ✅ `org.springaicommunity.agents.judge.score` - Score types (~65%)

### Moderate Coverage (30-60%)
- 🟡 `org.springaicommunity.agents.judge.llm` - LLM judges (~45%)
- 🟡 `org.springaicommunity.agents.judge.result` - Judgment and checks (~40%)
- 🟡 `org.springaicommunity.agents.judge.context` - Judgment context (~38%)

### Low Coverage (<30%)
- 🔴 `org.springaicommunity.agents.judge.jury` - Jury system (~15%)
  - SimpleJury: 0%
  - Verdict: 0%
  - VotingStrategies: 0%
  - Juries utility: 0%
  - MetaJury: 0%
- 🔴 `org.springaicommunity.agents.judge.agent` - Agent judge (0%)
- 🔴 `org.springaicommunity.agents.judge` - Judge composition (~20%)

## Uncovered Classes (0% coverage)

### NEW in Phases 1-6 (Jury Refactoring)
1. **JudgmentStatus** (enum) - NEW
2. **ErrorPolicy** (enum) - NEW
3. **TiePolicy** (enum) - NEW
4. **Scores** (utility) - NEW
5. **Verdict.Builder** - NEW
6. **Juries** (utility) - NEW
7. **MetaJury** - NEW
8. **JuryAdvisor** - NEW (in advisors module)

### Existing Gaps
9. **MajorityVotingStrategy** - Updated with policies
10. **AverageVotingStrategy**
11. **WeightedAverageStrategy**
12. **MedianVotingStrategy**
13. **ConsensusStrategy**
14. **SimpleJury** - Refactored
15. **AgentJudge**
16. **Judgment.Builder** - Enhanced
17. **AsyncJudge**
18. **ReactiveJudge**

## Priority Test Implementation

### Phase 1: Critical Gaps (P0) - Target: +30% coverage
**Focus on NEW jury refactoring classes**

1. **JudgmentStatusTest** ⚠️ MISSING
   - Enum values, pass() convenience method
   - Estimated impact: +2% line coverage

2. **ScoresTest** ⚠️ MISSING (NEW utility)
   - toNormalized() with all score types
   - Null handling, edge cases
   - Estimated impact: +3% line coverage

3. **VotingStrategy Tests** (5 classes) ⚠️ ALL MISSING
   - MajorityVotingStrategyTest (with TiePolicy, ErrorPolicy)
   - AverageVotingStrategyTest
   - WeightedAverageStrategyTest
   - MedianVotingStrategyTest
   - ConsensusStrategyTest
   - Estimated impact: +15% line coverage

4. **SimpleJuryTest** ⚠️ MISSING
   - Parallel/sequential execution
   - Identity preservation (individualByName)
   - Estimated impact: +5% line coverage

5. **VerdictTest** ⚠️ MISSING
   - Builder pattern, immutability
   - subVerdicts for MetaJury
   - Estimated impact: +3% line coverage

6. **JuriesTest** ⚠️ MISSING
   - fromJudges() with auto-naming
   - Duplicate name handling
   - combine(), allOf()
   - Estimated impact: +2% line coverage

**Phase 1 Total Estimated Coverage: 69.3%** (39.3% → 69.3%)

### Phase 2: Foundation Enhancement (P1) - Target: +10% coverage

7. **JudgmentTest** - Enhance existing
   - Builder validation, factory methods
   - Metadata accessors (elapsed(), error())
   - Estimated impact: +3% line coverage

8. **JudgmentContextTest** - Enhance existing
   - All builder options, immutability
   - Estimated impact: +2% line coverage

9. **MetaJuryTest** ⚠️ MISSING
   - Sub-jury aggregation
   - Sub-verdicts preservation
   - Estimated impact: +2% line coverage

10. **AgentJudgeTest** ⚠️ MISSING
    - Pattern extraction (PASS, SCORE, REASONING)
    - AgentClient integration
    - Estimated impact: +3% line coverage

**Phase 2 Total Estimated Coverage: 79.3%** (69.3% → 79.3%)

### Phase 3: Integration & Advanced (P2) - Target: +10% coverage

11. **JuryAdvisorTest** ⚠️ MISSING
    - AgentClient integration
    - Response context attachment
    - Estimated impact: +2% line coverage

12. **JudgesTest** - Enhance existing
    - named(), tryMetadata()
    - Composition utilities
    - Estimated impact: +2% line coverage

13. **AsyncJudgeTest** ⚠️ MISSING
    - Async execution, timeout handling
    - Estimated impact: +3% line coverage

14. **ReactiveJudgeTest** ⚠️ MISSING
    - Reactive streams, backpressure
    - Estimated impact: +3% line coverage

**Phase 3 Total Estimated Coverage: 89.3%** (79.3% → 89.3%)

## Running Coverage Reports

### Generate HTML Report
```bash
./mvnw test jacoco:report -pl agent-models/spring-ai-agents-judge
```

**Report location**: `agent-models/spring-ai-agents-judge/target/site/jacoco/index.html`

### Check Coverage Thresholds
```bash
./mvnw test jacoco:check -pl agent-models/spring-ai-agents-judge
```

**Current thresholds** (will fail build if not met):
- Line coverage: ≥39%
- Branch coverage: ≥25%

### View Coverage Summary
```bash
cat agent-models/spring-ai-agents-judge/target/site/jacoco/jacoco.csv
```

## Coverage Improvement Strategy

### Incremental Threshold Updates
As tests are added, update thresholds in `pom.xml`:

```xml
<configuration>
    <rules>
        <rule>
            <element>BUNDLE</element>
            <limits>
                <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.70</minimum> <!-- Increase after Phase 1 -->
                </limit>
                <limit>
                    <counter>BRANCH</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.60</minimum> <!-- Increase after Phase 1 -->
                </limit>
            </limits>
        </rule>
    </rules>
</configuration>
```

### CI/CD Integration
- ✅ JaCoCo configured with coverage thresholds
- ✅ Reports generated on every test run
- 🔲 Coverage badge in README (future)
- 🔲 Trend tracking over time (future)

## Next Steps

1. **Implement Phase 1 tests** (Voting strategies, Jury core) → Target: 69% coverage
2. **Update thresholds** to 65% line, 55% branch
3. **Implement Phase 2 tests** (Foundation enhancement) → Target: 79% coverage
4. **Update thresholds** to 75% line, 65% branch
5. **Implement Phase 3 tests** (Integration) → Target: 89% coverage
6. **Final threshold** to 85% line, 80% branch
7. **Maintain** with test-first development for new features

---

**Last Updated**: After Phases 1-6 (Jury Refactoring)
**JaCoCo Version**: 0.8.11
**Maven Command**: `./mvnw test jacoco:report`
