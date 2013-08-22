package ca.ubc.cs.beta.aclib.initialization.doublingcapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aclib.exceptions.OutOfTimeException;
import ca.ubc.cs.beta.aclib.initialization.InitializationProcedure;
import ca.ubc.cs.beta.aclib.misc.MapList;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.random.SeedableRandomPool;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aclib.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aclib.seedgenerator.SetInstanceSeedGenerator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueue;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueueResultContext;
import ca.ubc.cs.beta.aclib.termination.TerminationCondition;

public class DoublingCappingInitializationProcedure implements InitializationProcedure {

	private final ThreadSafeRunHistory runHistory;
	private final ParamConfiguration initialIncumbent;
	private final TargetAlgorithmEvaluator tae;
	private final DoublingCappingInitializationProcedureOptions opts;
	private final Logger log = LoggerFactory.getLogger(DoublingCappingInitializationProcedure.class);
	private final int maxIncumbentRuns;
	private final List<ProblemInstance> instances;
	private final InstanceSeedGenerator insc;
	private final ParamConfiguration incumbent;
	private final TerminationCondition termCond;
	private final double cutoffTime;
	private final SeedableRandomPool pool;
	private boolean deterministicInstanceOrdering;
	private final ParamConfigurationSpace configSpace;
	
	private final int numberOfChallengers;
	private final int numberOfRunsPerChallenger;
	

	public DoublingCappingInitializationProcedure(ThreadSafeRunHistory runHistory, ParamConfiguration initialIncumbent, TargetAlgorithmEvaluator tae, DoublingCappingInitializationProcedureOptions opts, InstanceSeedGenerator insc, List<ProblemInstance> instances,  int maxIncumbentRuns , TerminationCondition termCond, double cutoffTime, SeedableRandomPool pool, boolean deterministicInstanceOrdering)
	{
		this.runHistory =runHistory;
		this.initialIncumbent = initialIncumbent;
		this.tae = tae;
		this.opts = opts;
		this.instances = instances;
		this.maxIncumbentRuns = maxIncumbentRuns;
		this.insc = insc;
		this.incumbent = initialIncumbent;
		this.termCond = termCond;
		this.cutoffTime = cutoffTime;
		this.pool = pool;
		this.deterministicInstanceOrdering = deterministicInstanceOrdering;
		this.configSpace = initialIncumbent.getConfigurationSpace();
		
		this.numberOfChallengers = opts.numberOfChallengers;
		this.numberOfRunsPerChallenger = opts.numberOfRunsPerChallenger;
		
	}
	
	@Override
	public void run()
	{
		
		log.info("Using Doubling Capping Initialization");
		ParamConfiguration incumbent = this.initialIncumbent;
		log.info("Configuration Set as initial Incumbent: {}", incumbent);
		
		
		
		double startKappa=cutoffTime;
		//Start kappa at the lowest value that is greater than 1, and perfectly divisible from kappaMax.
		
		int divisions = 1;
		while(startKappa/2 > 1)
		{
			startKappa/=2;
			divisions++;
		}
	
		
		Set<ParamConfiguration> randomConfigurations = new HashSet<ParamConfiguration>();
		
		int totalFirstRoundChallengers = numberOfChallengers * numberOfRunsPerChallenger * divisions;
		if(totalFirstRoundChallengers > configSpace.getUpperBoundOnSize())
		{
			throw new IllegalStateException("Doubling Capping initialization won't work with this configuration space as it's too small, use classic");
		}
		
		
		
		//Get enough random configurations for the first round
		Random configRandom = pool.getRandom("DOUBLING_INITIALIZATION_CONFIGS");
		while(randomConfigurations.size() < totalFirstRoundChallengers)
		{
			randomConfigurations.add(configSpace.getRandomConfiguration(configRandom));
		}
		
		List<ProblemInstanceSeedPair> pisps = new ArrayList<ProblemInstanceSeedPair>(totalFirstRoundChallengers);
		
		//Generate enough problem instance seed pairs for the first round
		//If we make 10000 attempts without getting a 
		Random pispRandom = pool.getRandom("DOUBLING_INITIALIZATION_PISPS");
		for(int i=0, attempts=0; i < totalFirstRoundChallengers; i++, attempts++)
		{
			if(insc instanceof SetInstanceSeedGenerator)
			{
				insc.reinit();
			}
			
			ProblemInstance pi =instances.get(pispRandom.nextInt(instances.size()));
			
			if(!insc.hasNextSeed(pi))
			{
				i--;
				if(attempts > 10000)
				{
					throw new IllegalStateException("Could not generate anymore problem instance seed pairs, probably the number of instances * number of seeds is too small compared to the number of challengers and runs per challenge to use in initialization");
				}
				continue; 
			} else
			{
				attempts=0;
			}
			
		
			ProblemInstanceSeedPair pisp = new ProblemInstanceSeedPair(pi,insc.getNextSeed(pi));
			
			pisps.add(pisp);
			
		}
		
		
		/**
		 * Construct a giant queue of runConfigs to do essentially everything we will do in the first round
		 * (That is over all configurations, all problem instance seed pairs, all cutofftimes)
		 * 
		 */
		BasicTargetAlgorithmEvaluatorQueue taeQueue = new BasicTargetAlgorithmEvaluatorQueue(tae, true);
		
		LinkedBlockingQueue<ParamConfiguration> configsQueue = new LinkedBlockingQueue<ParamConfiguration>();
		configsQueue.addAll(randomConfigurations);
		
		LinkedBlockingQueue<ProblemInstanceSeedPair> pispsQueue = new LinkedBlockingQueue<ProblemInstanceSeedPair>();
		pispsQueue.addAll(pisps);
		
		LinkedBlockingQueue<RunConfig> runsToDo = new LinkedBlockingQueue<RunConfig>();
		
		for(double kappa = startKappa ; kappa <= cutoffTime; kappa*=2)
		{
			for(int i=0; i <= numberOfChallengers * numberOfRunsPerChallenger; i++)
			{
				ParamConfiguration config;
				if(i == 0)
				{
					config = initialIncumbent;
				} else
				{
					
					config = configsQueue.poll();
					
				}
				RunConfig rc = new RunConfig(pispsQueue.poll(), kappa, config, kappa < cutoffTime);
				
				runsToDo.add(rc);
			}
		}
	

		 final AtomicBoolean allRunsCompleted = new AtomicBoolean(false);
		
		TargetAlgorithmEvaluatorRunObserver obs = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends KillableAlgorithmRun> runs) {
				if(allRunsCompleted.get())
				{
					for(KillableAlgorithmRun run : runs)
					{
						run.kill();
					}
				}
				
			}
			
			
		};
		
		
		
		
		
		/**
		 * Essentially this block of code is doing the following:
		 * 
		 * While completed runs < numberOfChallengers.
		 * 			
		 * 		Take the next runToDo (if it is the incumbent and we have a solved run, skip it).
		 * 		Schedule the run
		 * 		While there are runs that are done:
		 * 			If the run is SAT or UNSAT increase the number of completed runs (and keep track if it was the incumbent). 
		 * 			If the run is anything else and has cutoff of Kappa Max then increase the number of completed runs.
		 * 		
		 * 			
		 * 			 
		 * 
		 * 
		 * 
		 * 
		 */
		
		int completedRuns = 0;
		AtomicBoolean incumbentSolved = new AtomicBoolean(false);
		
		while(runsToDo.peek() != null)
		{
			
			try 
			{
				RunConfig rc = runsToDo.take();
				
				
				
				while((incumbentSolved.get() == true) && rc.getParamConfiguration().equals(initialIncumbent))
				{
					rc = runsToDo.take();
				}
					
				taeQueue.evaluateRunAsync(Collections.singletonList(rc), obs);
				
			
				BasicTargetAlgorithmEvaluatorQueueResultContext context = taeQueue.poll();
				
				
				
				
				MapList<RunResult, AlgorithmRun> runs = new MapList<RunResult,AlgorithmRun>(new EnumMap<RunResult,List<AlgorithmRun>>(RunResult.class));
				
				while(context != null)
				{
					AlgorithmRun run = context.getAlgorithmRuns().get(0);
					
					switch(run.getRunResult())
					{
					
					
						case SAT:
						case UNSAT:
							completedRuns++;
							if(run.getRunConfig().getParamConfiguration().equals(initialIncumbent))
							{
								completedRuns--;
								incumbentSolved.set(true);
							}
							runs.addToList(run.getRunResult(), run);
						case TIMEOUT:
							if(!run.getRunConfig().hasCutoffLessThanMax())
							{
								if(run.getRunConfig().getParamConfiguration().equals(initialIncumbent))
								{
									completedRuns--;
									incumbentSolved.set(true);
								}
								completedRuns++;
								runs.addToList(run.getRunResult(), run);
							}
						case KILLED:
							  log.debug("Killed run detected in First round ");
							break;
							
							
							
						case CRASHED:
							if(!run.getRunConfig().hasCutoffLessThanMax())
							{
								if(run.getRunConfig().getParamConfiguration().equals(initialIncumbent))
								{
									incumbentSolved.set(true);
								}
								completedRuns++;
								runs.addToList(run.getRunResult(), run);
							}
							
							break;
						default:
							throw new IllegalStateException("Got unexpected run result back " + context.getAlgorithmRuns().get(0).getRunResult());
					}
				
					context = taeQueue.poll();
				}
			} catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
				return;
			}
		}
		
			
			
			
		
		
		
		//Generate all the random instances required
		
		
		
	}

	@Override
	public ParamConfiguration getIncumbent() {
		return incumbent;
	}
	
	/**
	 * Evaluates a single run, and updates our runHistory
	 * @param runConfig
	 * @return
	 */
	protected List<AlgorithmRun> evaluateRun(RunConfig runConfig)
	{
		return evaluateRun(Collections.singletonList(runConfig));
	}
	
	/**
	 * Evaluates a list of runs and updates our runHistory
	 * @param runConfigs
	 * @return
	 */
	protected List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs)
	{
	
		if(termCond.haveToStop())
		{
			throw new OutOfTimeException();
		}
		log.info("Initialization: Scheduling {} run(s):",  runConfigs.size());
		for(RunConfig rc : runConfigs)
		{
			Object[] args = {  runHistory.getThetaIdx(rc.getParamConfiguration())!=-1?" "+runHistory.getThetaIdx(rc.getParamConfiguration()):"", rc.getParamConfiguration(), rc.getProblemInstanceSeedPair().getInstance().getInstanceID(),  rc.getProblemInstanceSeedPair().getSeed(), rc.getCutoffTime()};
			log.info("Initialization: Scheduling run for config{} ({}) on instance {} with seed {} and captime {}", args);
		}
		
		List<AlgorithmRun> completedRuns = tae.evaluateRun(runConfigs);
		
		for(AlgorithmRun run : completedRuns)
		{
			RunConfig rc = run.getRunConfig();
			Object[] args = {  runHistory.getThetaIdx(rc.getParamConfiguration())!=-1?" "+runHistory.getThetaIdx(rc.getParamConfiguration()):"", rc.getParamConfiguration(), rc.getProblemInstanceSeedPair().getInstance().getInstanceID(),  rc.getProblemInstanceSeedPair().getSeed(), rc.getCutoffTime(), run.getResultLine(),  run.getWallclockExecutionTime()};
			log.info("Initialization: Completed run for config{} ({}) on instance {} with seed {} and captime {} => Result: {}, wallclock time: {} seconds", args);
		}
		
		
		
		updateRunHistory(completedRuns);
		return completedRuns;
	}
	
	/**
	 * 
	 * @return the input parameter (unmodified, simply for syntactic convience)
	 */
	protected List<AlgorithmRun> updateRunHistory(List<AlgorithmRun> runs)
	{
		for(AlgorithmRun run : runs)
		{
			try {
					runHistory.append(run);
			} catch (DuplicateRunException e) {
				//We are trying to log a duplicate run
				throw new IllegalStateException(e);
			}
		}
		return runs;
	}
	


}
