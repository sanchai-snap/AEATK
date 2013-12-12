package ca.ubc.cs.beta.aclib.runhistory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.objectives.OverallObjective;
import ca.ubc.cs.beta.aclib.objectives.RunObjective;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;

public class ThreadSafeRunHistoryWrapper implements ThreadSafeRunHistory {

	private final RunHistory runHistory;
	
	ReadWriteLockThreadTracker rwltt = new ReadWriteLockThreadTracker();
	
	//private final static Logger log = LoggerFactory.getLogger(ThreadSafeRunHistoryWrapper.class);
	
	public ThreadSafeRunHistoryWrapper(RunHistory runHistory)
	{
		this.runHistory = runHistory;
	}
	
	@Override
	public void append(Collection<AlgorithmRun> runs)
			throws DuplicateRunException {

	
		lockWrite();
		
		try {
			for(AlgorithmRun run : runs)
			{
				//log.debug("Atomically appending run {} " + run.getRunConfig());
				runHistory.append(run);
			}
			
		} finally
		{
			unlockWrite();
		}
		
	}
	
	
	@Override
	public void append(AlgorithmRun run) throws DuplicateRunException {
		
		
		lockWrite();
		try {
			//log.debug("Appending single run {} " + run.getRunConfig());
			runHistory.append(run);
		} finally
		{
			unlockWrite();
		}
		
		
	}

	@Override
	public RunObjective getRunObjective() {
		lockRead();
		
		try {
			return runHistory.getRunObjective();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public OverallObjective getOverallObjective() {
		lockRead();
		try {
			return runHistory.getOverallObjective();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public void incrementIteration() {
		lockWrite();
		
		try {
			 runHistory.incrementIteration();
		} finally
		{
			unlockWrite();
		}
	}

	@Override
	public int getIteration() {

		lockRead();
		try {
			return runHistory.getIteration();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ProblemInstance> getProblemInstancesRan(ParamConfiguration config) {
		lockRead();
		try {
			return runHistory.getProblemInstancesRan(config);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ProblemInstanceSeedPair> getProblemInstanceSeedPairsRan(
			ParamConfiguration config) {
		
		lockRead();
		try {
			return runHistory.getProblemInstanceSeedPairsRan(config);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues,
			double minimumResponseValue) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues, minimumResponseValue);
		} finally
		{
			unlockRead();
		}
	}




	@Override
	public double getTotalRunCost() {
		lockRead();
		try {
			return runHistory.getTotalRunCost();
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public Set<ProblemInstance> getUniqueInstancesRan() {
		lockRead();
		try {
			return runHistory.getUniqueInstancesRan();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ParamConfiguration> getUniqueParamConfigurations() {
		lockRead();
		try {
			return runHistory.getUniqueParamConfigurations();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public int[][] getParameterConfigurationInstancesRanByIndex() {
		lockRead();
		try {
			return runHistory.getParameterConfigurationInstancesRanByIndex();
		} finally
		{
			unlockRead();
		}
	}


	
	@Override
	public List<ParamConfiguration> getAllParameterConfigurationsRan() {
		lockRead();
		try {
			return runHistory.getAllParameterConfigurationsRan();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double[][] getAllConfigurationsRanInValueArrayForm() {
		lockRead();
		try {
			return runHistory.getAllConfigurationsRanInValueArrayForm();
		} finally
		{
			unlockRead();
		}
	}


	@Override
	public List<RunData> getAlgorithmRunData() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunData();
		} finally
		{
			unlockRead();
	
		}
		
	
	}



	@Override
	public Set<ProblemInstanceSeedPair> getEarlyCensoredProblemInstanceSeedPairs(
			ParamConfiguration config) {
		lockRead();
		try {
			return runHistory.getEarlyCensoredProblemInstanceSeedPairs(config);
		} finally
		{
			unlockRead();
	
		}
	}



	@Override
	public int getThetaIdx(ParamConfiguration configuration) {
		lockRead();
		try {
			return runHistory.getThetaIdx(configuration);
		} finally
		{
			unlockRead();
	
		}
	}

	@Override
	public double getEmpiricalCost(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			double minimumResponseValue) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime);
		} finally
		{
			unlockRead();
	
		}
	}

	@Override
	public int getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(
			ParamConfiguration config) {
		lockRead();
		try {
			return runHistory.getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(config);
		} finally
		{
			unlockRead();
	
		}
	}


	@Override
	public void readLock() {
		lockRead();
	}


	@Override
	public void releaseReadLock() {
		unlockRead();
		
	}

	@Override
	public List<AlgorithmRun> getAlgorithmRunsExcludingRedundant(ParamConfiguration config) {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsExcludingRedundant(config);
		} finally
		{
			unlockRead();
	
		}
	}
	

	@Override
	public int getTotalNumRunsOfConfigExcludingRedundant(ParamConfiguration config) {
		lockRead();
		try {
			return runHistory.getTotalNumRunsOfConfigExcludingRedundant(config);
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRun> getAlgorithmRunsExcludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsExcludingRedundant();
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRun> getAlgorithmRunsIncludingRedundant(ParamConfiguration config) {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsIncludingRedundant(config);
		} finally
		{
			unlockRead();
	
		}
	}
	

	@Override
	public int getTotalNumRunsOfConfigIncludingRedundant(ParamConfiguration config) {
		lockRead();
		try {
			return runHistory.getTotalNumRunsOfConfigIncludingRedundant(config);
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRun> getAlgorithmRunsIncludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsIncludingRedundant();
		} finally
		{
			unlockRead();
		}
	}
	
	

	@Override
	public Map<ProblemInstance, LinkedHashMap<Long, Double>> getPerformanceForConfig(
			ParamConfiguration configuration) {
		lockRead();
		try {
			return runHistory.getPerformanceForConfig(configuration);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public List<Long> getSeedsUsedByInstance(ProblemInstance pi) {
		lockRead();
		try {
			return runHistory.getSeedsUsedByInstance(pi);
		} finally
		{
			unlockRead();
		}
	}
	
	
	public void lockRead()
	{
		this.rwltt.lockRead();
	
	}
	
	private void unlockRead()
	{
		this.rwltt.unlockRead();
	}
	
	private void lockWrite()
	{
		this.rwltt.lockWrite();
	}
	
	private void unlockWrite()
	{
		this.rwltt.unlockWrite();
		
	}

	@Override
	public int getOrCreateThetaIdx(ParamConfiguration config) {
		lockWrite();
		try {
			return this.runHistory.getOrCreateThetaIdx(config);
		} finally
		{
			unlockWrite();
		}
	
		
	}

	@Override
	public double getEmpiricalCostLowerBound(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return this.runHistory.getEmpiricalCostLowerBound(config, instanceSet, cutoffTime);
	}

	@Override
	public double getEmpiricalCostUpperBound(ParamConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return this.runHistory.getEmpiricalCostUpperBound(config, instanceSet, cutoffTime);
	}


	

	

}
