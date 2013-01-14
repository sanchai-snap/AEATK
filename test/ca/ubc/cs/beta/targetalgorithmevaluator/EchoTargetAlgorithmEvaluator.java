package ca.ubc.cs.beta.targetalgorithmevaluator;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.ExistingAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractBlockingTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.currentstatus.CurrentRunStatusObserver;

/**
 * Faster way of echoing results back
 * Only works with the paramEchoParamFile
 * 
 * 
 * @author Steve Ramage 
 *
 */
public class EchoTargetAlgorithmEvaluator  extends AbstractBlockingTargetAlgorithmEvaluator  implements TargetAlgorithmEvaluator{

	public EchoTargetAlgorithmEvaluator(AlgorithmExecutionConfig execConfig) {
		super(execConfig);
		// TODO Auto-generated constructor stub
	}


    
	@Deprecated
	public volatile double wallClockTime = 0;
	
	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs) {
		return evaluateRun(runConfigs, null);
	}

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, CurrentRunStatusObserver obs) {
		
		List<AlgorithmRun> results = new ArrayList<AlgorithmRun>();
		
		
		 /*
		 * solved { SAT, UNSAT, TIMEOUT, CRASHED, ABORT, INVALID } [SAT]
				 * runtime [-1,1000] [0]
				 * runlength [0,1000000][0]
				 * quality [0, 1000000] [0]
				 * seed [ -1,4294967295][1]i
		*/		 
		for(RunConfig rc : runConfigs)
		{
			StringBuilder sb = new StringBuilder();
			
			ParamConfiguration config = rc.getParamConfiguration();
			
			sb.append(config.get("solved")).append(",");
			sb.append(config.get("runtime")).append(",");
			sb.append(config.get("runlength")).append(",");
			sb.append(config.get("quality")).append(",");
			sb.append(config.get("seed"));
			
			
			
			
			
			results.add(new ExistingAlgorithmRun(execConfig, rc, sb.toString(),wallClockTime));
			
			
			
		}
		
		return results;
	}

	@Override
	public boolean isRunFinal() {
		return true;
	}

	@Override
	public boolean areRunsPersisted() {
		return false;
	}

	@Override
	protected void subtypeShutdown() {
		
		
	}

	@Override
	public boolean areRunsObservable() {
		return false;
	}

	
}
