package ca.ubc.cs.beta.aclib.targetalgorithmevaluator.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.ExistingAlgorithmRun;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.random.SeedableRandomSingleton;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluator;

public class RandomResponseTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluator {

	double scale = 1.0; 
	public RandomResponseTargetAlgorithmEvaluator(
			AlgorithmExecutionConfig execConfig) {
		super(execConfig);
		scale = Math.abs(Double.valueOf(execConfig.getAlgorithmExecutable()));
		
	}

	@Override
	public void notifyShutdown() {

	}

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs) {
		Random rand = SeedableRandomSingleton.getRandom();
		
		List<AlgorithmRun> ar = new ArrayList<AlgorithmRun>(runConfigs.size());
		for(RunConfig rc : runConfigs)
		{
			ar.add(new ExistingAlgorithmRun(execConfig, rc, "SAT, " + rand.nextDouble()*scale + ",-1,0," + rc.getProblemInstanceSeedPair().getSeed()));
		}
		
		return ar;
	}

}
