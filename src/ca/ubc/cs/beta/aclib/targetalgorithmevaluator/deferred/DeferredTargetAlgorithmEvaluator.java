package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.deferred;

import java.util.List;

import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;

/**
 * A Target Algorithm Evaluator that also supports asynchronously scheduling runs.
 * <p>
 * <b>Implementation Note:</b> It's not clear at this time whether pending callbacks and currently
 * executing runs should prevent the JVM from exiting as a rule. You should use your judgement (i.e. 
 * if your TAE is database driven, and you can get the results later, maybe not. If you are simply
 * using this mechanism as a replacement for synchronous execution, then maybe yes.
 * 
 * 
 * 
 * @author Steve Ramage 
 *
 */
public interface DeferredTargetAlgorithmEvaluator extends
		TargetAlgorithmEvaluator {

	/**
	 * Evaluates the given configuration, and when complete the handler is invoked
	 * @param runConfig  run configuration to evaluate
	 * @param handler    handler to invoke on completion or failure
	 */
	public void evaluateRunsAsync(RunConfig runConfig, DeferredTargetAlgorithmEvaluatorHandler handler );
	
	/**
	 * Evaluates the given configuration, and when complete the handler is invoked
	 * @param runConfigs list of run configuration to evaluate
	 * @param handler    handler to invoke on completion or failure
	 */
	public void evaluateRunsAsync(List<RunConfig> runConfigs, DeferredTargetAlgorithmEvaluatorHandler handler);
	
	
	
}
