package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.deferred.TAECallback;

/**
 * Retries crashed runs some number of times
 * 
 * This should be transparent to the end user, so all runs must appear in order, and the run count should not show the retried runs.

 * @author Steve Ramage 
 *
 */
@ThreadSafe
public class RetryCrashedRunsTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	private AtomicInteger runCount = new AtomicInteger(0);
	private final int retryCount; 
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public RetryCrashedRunsTargetAlgorithmEvaluator(int retryCount, TargetAlgorithmEvaluator tae) {
		super(tae);
		if(retryCount < 0)
		{
			throw new IllegalArgumentException("Retry Count should be atleast 0");
		}
		this.retryCount = retryCount;
		
		if(tae.isRunFinal())
		{
			log.warn("Target Algorithm Evaluator {} issues final runs, retrying will be a waste of time", tae.getClass().getSimpleName());
		}
	}
	
	@Override
	public List<AlgorithmRun> evaluateRun(RunConfig run) {
		return this.evaluateRun(Collections.singletonList(run));
	}

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs) {
		List<AlgorithmRun> runs = tae.evaluateRun(runConfigs);
		
		runs = new ArrayList<AlgorithmRun>(runs);
		
		
		for(int i=1; i <= retryCount; i++)
		{
			Map<RunConfig, Integer> crashedRuns = new HashMap<RunConfig,Integer>();
			boolean crashedRunsExist = false;
			
			for(int j =0; j < runs.size(); j++)
			{
				AlgorithmRun run = runs.get(j); 
				if(run.getRunResult().equals(RunResult.CRASHED))
				{
					crashedRuns.put(run.getRunConfig(),j);
					crashedRunsExist = true;
				}
			}
			
			
			if(!crashedRunsExist)
			{
				log.debug("No crashed runs to retry");
				break;
			} else
			{
				log.info("Retrying {} crashed runs (Attempt {})", crashedRuns.size(), i);
			}
			
			List<RunConfig> crashRCs = new ArrayList<RunConfig>(crashedRuns.keySet().size());
			crashRCs.addAll(crashedRuns.keySet());
			
			
			List<AlgorithmRun> retriedRuns = tae.evaluateRun(crashRCs);
			
			
			for(AlgorithmRun run : retriedRuns)
			{
				runs.set(crashedRuns.get(run.getRunConfig()), run);
			}
		}	
		
		runCount.addAndGet(runs.size());
		return runs;
		
		
		
	}

	@Override
	public int getRunCount() {
		//Override this because internal TAE's have probably seen more runs
		return runCount.get();
	}

	@Override
	public void seek(List<AlgorithmRun> runs)
	{
		tae.seek(runs);
		runCount.addAndGet(runs.size());
	}

	@Override
	public void evaluateRunsAsync(RunConfig runConfig, TAECallback handler) {
		throw new UnsupportedOperationException("Can't retry runs that are asynchronous atm");
		
	}

	@Override
	public void evaluateRunsAsync(List<RunConfig> runConfigs,
			TAECallback handler) {
		throw new UnsupportedOperationException("Can't retry runs that are asynchronous atm");
		
	}
	

}
