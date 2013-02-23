package ca.ubc.cs.beta.targetalgorithmevaluator;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ca.ubc.cs.beta.TestHelper;
import ca.ubc.cs.beta.aclib.algorithmrun.AlgorithmRun;
import ca.ubc.cs.beta.aclib.algorithmrun.RunResult;
import ca.ubc.cs.beta.aclib.algorithmrun.kill.KillableAlgorithmRun;
import ca.ubc.cs.beta.aclib.configspace.ParamConfiguration;
import ca.ubc.cs.beta.aclib.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.aclib.exceptions.TargetAlgorithmAbortException;
import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.misc.logback.MarkerFilter;
import ca.ubc.cs.beta.aclib.misc.logging.LoggingMarker;
import ca.ubc.cs.beta.aclib.misc.random.SeedableRandomSingleton;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aclib.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.CommandLineTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.currentstatus.CurrentRunStatusObserver;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbortOnCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbortOnFirstRunCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.TimingCheckerTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.EqualTargetAlgorithmEvaluatorTester;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.VerifySATTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.targetalgorithmevaluator.massiveoutput.MassiveOutputParamEchoExecutor;


public class DynamicCappingTestSet {

	
	private static TargetAlgorithmEvaluator tae;
	
	private static AlgorithmExecutionConfig execConfig;
	
	private static ParamConfigurationSpace configSpace;
	
	private static final int TARGET_RUNS_IN_LOOPS = 10;
	@BeforeClass
	public static void beforeClass()
	{
		File paramFile = TestHelper.getTestFile("paramFiles/paramEchoParamFile.txt");
		configSpace = new ParamConfigurationSpace(paramFile);
	}
	Random r;
	

	
	
	PrintStream old;
	ByteArrayOutputStream bout;
	public void startOutputCapture()
	{
	
		bout = new ByteArrayOutputStream();
		old = System.out;
		System.setOut(new PrintStream(bout));
	}
	
	
	public String stopOutputCapture()
	{
		System.setOut(old);
		String boutString = bout.toString();
		System.out.println(boutString);
		return boutString;
	}
	
	@Before
	public void beforeTest()
	{
		StringBuilder b = new StringBuilder();
		b.append("java -cp ");
		b.append(System.getProperty("java.class.path"));
		b.append(" ");
		b.append(ParamEchoExecutor.class.getCanonicalName());
		execConfig = new AlgorithmExecutionConfig(b.toString(), System.getProperty("user.dir"), configSpace, false, false, 500);
		
		tae = new CommandLineTargetAlgorithmEvaluator( execConfig, false);
		SeedableRandomSingleton.reinit();
		System.out.println("Seed" + SeedableRandomSingleton.getSeed());;
		this.r = SeedableRandomSingleton.getRandom();
		
		
		
	}
	
	
	public void assertDEquals(String d1, double d2, double delta)
	{
		assertDEquals(Double.valueOf(d1), d2, delta);
	}
	public void assertDEquals(String d1, String d2, double delta)
	{
		assertDEquals(Double.valueOf(d1), Double.valueOf(d2), delta);
	}
	
	
	public void assertDEquals(double d1, double d2, double delta)
	{
		if(d1 - d2 > delta) throw new AssertionError("Expected "  + (d1 - d2)+ " < " + delta);
		if(d2 - d1 > delta) throw new AssertionError("Expected "  + (d1 - d2)+ " < " + delta);
		
	}

	

	/**
	 * Tests whether warnings are generated for Algorithms exceeding there runtime
	 */
	@Test
	public void testDynamicAdaptiveCappingSingleRun()
	{
		
	
		
		StringBuilder b = new StringBuilder();
		b.append("java -cp ");
		b.append(System.getProperty("java.class.path"));
		b.append(" ");
		b.append(TrueSleepyParamEchoExecutor.class.getCanonicalName());
		execConfig = new AlgorithmExecutionConfig(b.toString(), System.getProperty("user.dir"), configSpace, false, false, 0.01);
		
		tae = new CommandLineTargetAlgorithmEvaluator( execConfig, false);	
		
		assertTrue(tae.areRunsObservable());
		configSpace.setPRNG(r);
		
		List<RunConfig> runConfigs = new ArrayList<RunConfig>(1);
		for(int i=0; i < 1; i++)
		{
			ParamConfiguration config = configSpace.getRandomConfiguration();
			config.put("runtime", "100");
			if(config.get("solved").equals("INVALID") || config.get("solved").equals("ABORT") || config.get("solved").equals("CRASHED") || config.get("solved").equals("TIMEOUT"))
			{
				//Only want good configurations
				i--;
				continue;
			} else
			{
				RunConfig rc = new RunConfig(new ProblemInstanceSeedPair(new ProblemInstance("TestInstance"), Long.valueOf(config.get("seed"))), 3000, config);
				runConfigs.add(rc);
			}
		}
		
		System.out.println("Performing " + runConfigs.size() + " runs");
		
		//StringWriter sw = new StringWriter();
		//ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		//PrintStream out = System.out;
		//System.setOut(new PrintStream(bout));
		
		CurrentRunStatusObserver obs = new CurrentRunStatusObserver()
		{
			
			@Override
			public void currentStatus(List<? extends KillableAlgorithmRun> runs) {
				
				double runtimeSum = 0.0; 
				for(AlgorithmRun run : runs)
				{
					runtimeSum += run.getRuntime();
				}
				
				//System.out.println(runtimeSum);
				if(runtimeSum > 3)
				{
					for(KillableAlgorithmRun run : runs)
					{
						run.kill();
					}
				}
			}
			
		};
		
		long startTime  = System.currentTimeMillis();
		List<AlgorithmRun> runs = tae.evaluateRun(runConfigs,obs);
		long endTime = System.currentTimeMillis();
		//System.setOut(out);
		//System.out.println(bout.toString());
		
		for(AlgorithmRun run : runs)
		{
			System.out.println(run.getResultLine());
			
			ParamConfiguration config  = run.getRunConfig().getParamConfiguration();
			
			if(run.getRunResult().equals(RunResult.TIMEOUT))
			{
				continue;
			}
			assertDEquals(config.get("runtime"), run.getRuntime(), 0.1);
			assertDEquals(config.get("runlength"), run.getRunLength(), 0.1);
			assertDEquals(config.get("quality"), run.getQuality(), 0.1);
			assertDEquals(config.get("seed"), run.getResultSeed(), 0.1);
			assertEquals(config.get("solved"), run.getRunResult().name());
			//This executor should not have any additional run data
			assertEquals("",run.getAdditionalRunData());
			

		}
		
		tae.notifyShutdown();
		
		assertTrue("Should have taken less than five seconds to run, it took " + (endTime - startTime)/1000.0 + " seconds", (endTime - startTime) < (long) 6000);
	}
	
	
	
	

	/**
	 * Tests whether warnings are generated for Algorithms exceeding there runtime
	 */
	@Test
	public void testDynamicAdaptiveCappingMultiRunSingleCore()
	{
		
	
		
		StringBuilder b = new StringBuilder();
		b.append("java -cp ");
		b.append(System.getProperty("java.class.path"));
		b.append(" ");
		b.append(TrueSleepyParamEchoExecutor.class.getCanonicalName());
		execConfig = new AlgorithmExecutionConfig(b.toString(), System.getProperty("user.dir"), configSpace, false, false, 0.01);
		
		tae = new CommandLineTargetAlgorithmEvaluator( execConfig, false);	
		
		assertTrue(tae.areRunsObservable());
		configSpace.setPRNG(r);
		
		List<RunConfig> runConfigs = new ArrayList<RunConfig>(10);
		for(int i=0; i < 10; i++)
		{
			ParamConfiguration config = configSpace.getRandomConfiguration();
			config.put("runtime", ""+(i+1));
			if(config.get("solved").equals("INVALID") || config.get("solved").equals("ABORT") || config.get("solved").equals("CRASHED") || config.get("solved").equals("TIMEOUT"))
			{
				//Only want good configurations
				i--;
				continue;
			} else
			{
				RunConfig rc = new RunConfig(new ProblemInstanceSeedPair(new ProblemInstance("TestInstance"), Long.valueOf(config.get("seed"))), 3000, config);
				runConfigs.add(rc);
			}
		}
		
		System.out.println("Performing " + runConfigs.size() + " runs");
		
		//StringWriter sw = new StringWriter();
		//ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		//PrintStream out = System.out;
		//System.setOut(new PrintStream(bout));
		
		CurrentRunStatusObserver obs = new CurrentRunStatusObserver()
		{
			
			@Override
			public void currentStatus(List<? extends KillableAlgorithmRun> runs) {
				
				double runtimeSum = 0.0; 
				for(AlgorithmRun run : runs)
				{
					runtimeSum += run.getRuntime();
				}
				
				//System.out.println(runtimeSum);
				if(runtimeSum > 5)
				{
					System.out.println("Issuing kill order on " + runtimeSum);
					for(KillableAlgorithmRun run : runs)
					{
						run.kill();
					}
				}
			}
			
		};
		
		long startTime  = System.currentTimeMillis();
		List<AlgorithmRun> runs = tae.evaluateRun(runConfigs,obs);
		long endTime = System.currentTimeMillis();
		//System.setOut(out);
		//System.out.println(bout.toString());
		
		for(AlgorithmRun run : runs)
		{
			System.out.println(run.getResultLine());
			
			ParamConfiguration config  = run.getRunConfig().getParamConfiguration();
			
			if(run.getRunResult().equals(RunResult.TIMEOUT))
			{
				continue;
			}
			assertDEquals(config.get("runtime"), run.getRuntime(), 0.1);
			assertDEquals(config.get("runlength"), run.getRunLength(), 0.1);
			assertDEquals(config.get("quality"), run.getQuality(), 0.1);
			assertDEquals(config.get("seed"), run.getResultSeed(), 0.1);
			assertEquals(config.get("solved"), run.getRunResult().name());
			//This executor should not have any additional run data
			assertEquals("",run.getAdditionalRunData());
			

		}
		
		tae.notifyShutdown();
		
		assertTrue("Should have taken less than five seconds to run, it took " + (endTime - startTime)/1000.0 + " seconds", (endTime - startTime) < (long) 6000);
	}
	

	/**
	 * Tests whether warnings are generated for Algorithms exceeding there runtime
	 */
	@Test
	public void testDynamicAdaptiveCappingMultiRunMultiCore()
	{
		
	
		
		StringBuilder b = new StringBuilder();
		b.append("java -cp ");
		b.append(System.getProperty("java.class.path"));
		b.append(" ");
		b.append(TrueSleepyParamEchoExecutor.class.getCanonicalName());
		execConfig = new AlgorithmExecutionConfig(b.toString(), System.getProperty("user.dir"), configSpace, false, false, 0.01);
		
		tae = new CommandLineTargetAlgorithmEvaluator( execConfig, true);	
		
		assertTrue(tae.areRunsObservable());
		configSpace.setPRNG(r);
		
		List<RunConfig> runConfigs = new ArrayList<RunConfig>(10);
		for(int i=0; i < 10; i++)
		{
			ParamConfiguration config = configSpace.getRandomConfiguration();
			config.put("runtime", ""+(i+1));
			if(config.get("solved").equals("INVALID") || config.get("solved").equals("ABORT") || config.get("solved").equals("CRASHED") || config.get("solved").equals("TIMEOUT"))
			{
				//Only want good configurations
				i--;
				continue;
			} else
			{
				RunConfig rc = new RunConfig(new ProblemInstanceSeedPair(new ProblemInstance("TestInstance"), Long.valueOf(config.get("seed"))), 3000, config);
				runConfigs.add(rc);
			}
		}
		
		System.out.println("Performing " + runConfigs.size() + " runs");
		
		//StringWriter sw = new StringWriter();
		//ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		//PrintStream out = System.out;
		//System.setOut(new PrintStream(bout));
		
		CurrentRunStatusObserver obs = new CurrentRunStatusObserver()
		{
			
			@Override
			public void currentStatus(List<? extends KillableAlgorithmRun> runs) {
				
				double runtimeSum = 0.0; 
				for(AlgorithmRun run : runs)
				{
					runtimeSum += run.getRuntime();
				}
				
				//System.out.println(runtimeSum);
				if(runtimeSum > 5)
				{
					for(KillableAlgorithmRun run : runs)
					{
						run.kill();
					}
				}
			}
			
		};
		
		long startTime  = System.currentTimeMillis();
		List<AlgorithmRun> runs = tae.evaluateRun(runConfigs,obs);
		long endTime = System.currentTimeMillis();
		//System.setOut(out);
		//System.out.println(bout.toString());
		
		for(AlgorithmRun run : runs)
		{
			System.out.println(run.getResultLine());
			
			ParamConfiguration config  = run.getRunConfig().getParamConfiguration();
			
			if(run.getRunResult().equals(RunResult.TIMEOUT))
			{
				continue;
			}
			assertDEquals(config.get("runtime"), run.getRuntime(), 0.1);
			assertDEquals(config.get("runlength"), run.getRunLength(), 0.1);
			assertDEquals(config.get("quality"), run.getQuality(), 0.1);
			assertDEquals(config.get("seed"), run.getResultSeed(), 0.1);
			assertEquals(config.get("solved"), run.getRunResult().name());
			//This executor should not have any additional run data
			assertEquals("",run.getAdditionalRunData());
			

		}
		
		tae.notifyShutdown();
		
		assertTrue("Should have taken less than five seconds to run, it took " + (endTime - startTime)/1000.0 + " seconds", (endTime - startTime) < (long) 6000);
	}
	
	
	
	
}
