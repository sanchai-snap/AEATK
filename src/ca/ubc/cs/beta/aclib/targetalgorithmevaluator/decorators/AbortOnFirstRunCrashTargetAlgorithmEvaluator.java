package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jcip.annotations.ThreadSafe;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.exceptions.TargetAlgorithmAbortException;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.deferred.TAECallback;

/**
 * If the first run is a crash we will abort otherwise we ignore it
 * 
 * @author Steve Ramage 
 *
 */
@ThreadSafe
public class AbortOnFirstRunCrashTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	public AbortOnFirstRunCrashTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae) {
		super(tae);
		
	}
	
	@Override
	public List<AlgorithmRun> evaluateRun(RunConfig run) {
		return validate(super.evaluateRun(run));
	}

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs) {
		return validate(super.evaluateRun(runConfigs));
	}
	

	private final AtomicBoolean firstRunChecked = new AtomicBoolean(false);
	
	private List<AlgorithmRun> validate(List<AlgorithmRun> runs)
	{
		if(runs.size() == 0)
		{
			return runs;
		}
	
		if(firstRunChecked.getAndSet(true)) 
		{
			return runs;
		} else
		{		
		
			if(runs.get(0).getRunResult().equals(RunResult.CRASHED))
			{
				throw new TargetAlgorithmAbortException("First Run Crashed : " + runs.get(0).getRunConfig().toString()); 
			}
			
		}
		return runs;
		
	}

	@Override
	public void evaluateRunsAsync(RunConfig runConfig, TAECallback handler) {
		evaluateRunsAsync(Collections.singletonList(runConfig), handler);
	}

	@Override
	public void evaluateRunsAsync(List<RunConfig> runConfigs,
			final TAECallback handler) {
		
		
		TAECallback myHandler = new TAECallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRun> runs) {
				try {
					validate(runs);
					handler.onSuccess(runs);
				} catch(TargetAlgorithmAbortException e)
				{
					handler.onFailure(e);
				}
				
			}

			@Override
			public void onFailure(RuntimeException t) {
				handler.onFailure(t);
				
			}
			
		};
		
		tae.evaluateRunsAsync(runConfigs, myHandler);
	}
}
