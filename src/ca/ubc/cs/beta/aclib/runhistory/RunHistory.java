package ca.ubc.cs.beta.aclib.runhistory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.objectives.OverallObjective;
import ca.ubc.cs.beta.aclib.objectives.RunObjective;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.seedgenerator.InstanceSeedGenerator;

/**
 * Stores a complete listing of runs
 * 
 * NOTE: This class also does some other stuff like selecting new runs, perhaps this should be refactored
 * but we will see later
 * 
 * @author sjr
 *
 */
public interface RunHistory {

	/**
	 * Append a run to the RunHistory Object
	 * @param run	run to log
	 * @throws DuplicateRunException  - If a previous run has a duplicate config,instance and seed. NOTE: An exception will prevent the run from being logged, but the state of the RunHistory will still be consistent
	 */
	public void append(AlgorithmRun run) throws DuplicateRunException;
		
	/**
	 * Get the Run Objective we are opitimizing
	 * @return RunObjective we are optimizing
	 */
	public RunObjective getRunObjective();

	/**
	 * Get the Overall objective we are optimizing
	 * @return OverallObjective we are optimizing over
	 */
	public OverallObjective getOverallObjective();

	/**
	 * Increment the iteration we are storing runs with
	 */
	public void incrementIteration();

	
	/**
	 * Get the current iteration value
	 * @return current iteration
	 */
	public int getIteration();

	/**
	 * Return the set of instances we have run a ParamConfiguration on
	 * @param config configuration to get instances for
	 * @return	set instances that were run
	 */
	public Set<ProblemInstance> getInstancesRan(ParamConfiguration config);

	/**
	 * Returns the set of instance seed pairs we have run a Param Configuration on.
	 * @param config	configuration to get ProblemInstanceSeedPairs for
	 * @return	set of ProblemInstanceSeedPairs
	 */
	public Set<ProblemInstanceSeedPair> getAlgorithmInstanceSeedPairsRan(ParamConfiguration config);
	
	
	/**
	 * Compute and return the empirical cost of a parameter configuration on the subset of provided instances we have runs for
	 * @param config  		 ParamConfiguration to get Cost of
	 * @param instanceSet 	 instances to compute cost over
	 * @param cutoffTime 	 cutoff time for algorithm runs
	 * @return cost (Double.MAX_VALUE) if we haven't seen the configuration, otherwise the cost 
	 */
	public double getEmpiricalCost(ParamConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime);
	
	/**
	 * Compute and return the empirical cost of a parameter configuration on the subset of provided instances we have runs for.
	 * @param config 		ParamConfiguration to get Cost of
	 * @param instanceSet   Instances to compute cost over
	 * @param cutoffTime 	cutoff time for algorithm runs
	 * @return cost (Double.MAX_VALUE) if we haven't seen the configuration, otherwise the cost 
	 */
	double getEmpiricalCost(ParamConfiguration config, Set<ProblemInstance> instanceSet, double cutoffTime,	Map<ProblemInstance, Map<Long, Double>> hallucinatedValues);
	

	/**
	 * Returns a random instance with the fewest runs for the configuration
	 * @param config  		ParamConfiguration to run
	 * @param instanceList  List of problem instances.
	 * @param rand			Random object used to break ties
	 * @return random instance with the fewest runs for a configuration
	 */
	public ProblemInstance getRandomInstanceWithFewestRunsFor(ParamConfiguration config, List<ProblemInstance> instanceList, Random rand);

	/**
	 * Decides candidate Instance Seed Pairs to be run
	 * 
	 * NOTE: When ProblemInstanceSeedPairs are run from here we don't actually know they have been run until we get
	 * an append call. This needs to be fixed later (i.e. we may execute duplicate requests)
	 * 
	 * @param config 		 ParamConfiguration to run 
	 * @param instanceList 	 List of problem instances
	 * @param rand			 Random object used to break ties
	 * @return Random ProblemInstanceSeedPair object
	 */
	public ProblemInstanceSeedPair getRandomInstanceSeedWithFewestRunsFor(ParamConfiguration config, List<ProblemInstance> instanceList, Random rand);
	
	/**
	 * Returns the total number of runs for a configuration
	 * @param config ParamConfiguration
	 * @return number of runs
	 */
	public int getTotalNumRunsOfConfig(ParamConfiguration config);
	
	/**
	 * Returns the total cost of the all the runs used.
	 * @return total run cost (sum of all run times)
	 */
	public double getTotalRunCost();
	
	/**
	 * Returns a breakdown of each individual run cost
	 * @return double[] reporting the response value for every run, under the run objective
	 */
	public double[] getRunResponseValues();


	/**
	 * Get the set of Unique instances ran
	 * @return set of unique instances ran
	 */
	public Set<ProblemInstance> getUniqueInstancesRan();
	
	/**
	 * Gets a list of Unique Param Configurations Ran
	 * @return	set of param configurations ran
	 */
	public Set<ParamConfiguration> getUniqueParamConfigurations();
	
	/**
	 * Returns an Nx2 matrix where each row corresponds to a
	 * tuple of a param_configuration instance (in {@link getUniqueParamConfigurations()})
	 * and an instance (in {@link getUniqueInstancesRan()}). These represent the run configurations.
	 * @return array of entries of the form [thetaIdx, instanceIdx]
	 */
	public int[][] getParameterConfigurationInstancesRanByIndex();
	
	/**
	 * Returns an array containing a boolean for each run that tells us whether this run was capped or not.
	 * 
	 * @return boolean array signifying whether a run was capped
	 */
	public boolean[] getCensoredFlagForRuns();
	
	/**
	 * Returns the param configurations ran in order
	 * @return list of param configurations
	 */
	public List<ParamConfiguration> getAllParameterConfigurationsRan();
	
	/**
	 * Returns all configurations run in Value Array Form
	 * 
	 * @return array of double[] where each double is the paramconfiguration value Array. Indexed by thetaIdx
	 * 
	 */
	public double[][] getAllConfigurationsRanInValueArrayForm();
	
	/**
	 * Returns a list of all the runs
	 * 
	 * @return list of runs
	 */
	public List<AlgorithmRun> getAlgorithmRuns();
	
	/**
	 * Returns a list of all the Run Data
	 * 
	 * @return	list of run data
	 */
	public List<RunData> getAlgorithmRunData();

	/**
	 * Returns the Instance Seed Generator
	 * 
	 * @return instance seed generator
	 */
	public InstanceSeedGenerator getInstanceSeedGenerator();

	/**
	 * Returns a set of Instance Seed Pairs that were capped for a given configuration
	 * 
	 * @param config	paramconfiguration to select
	 * @return	set of instance seed pairs that are capped runs
	 */
	public Set<ProblemInstanceSeedPair> getCappedAlgorithmInstanceSeedPairs(ParamConfiguration config);

	
	double getEmpiricalPISPCost(ParamConfiguration config,
			Set<ProblemInstanceSeedPair> instanceSet, double cutoffTime);

	double getEmpiricalPISPCost(ParamConfiguration config,
			Set<ProblemInstanceSeedPair> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues);

	/**
	 * Returns the Index into arrays represented by this configuration
	 * 
	 * @param configuration 	Configuration needed (must exist in RunHistory)
	 * @return index into the theta array for this configuration
	 */
	public int getThetaIdx(ParamConfiguration configuration);

	
	
}