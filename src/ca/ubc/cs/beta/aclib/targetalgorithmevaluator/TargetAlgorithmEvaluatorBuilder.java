package ca.ubc.cs.beta.aclib.targetalgorithmevaluator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.aclib.options.ScenarioOptions;
import ca.ubc.cs.beta.aclib.options.TargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbortOnCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbortOnFirstRunCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.BoundedTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.SATConsistencyTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.TimingCheckerTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.LeakingMemoryTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.RetryCrashedRunsTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.RunHashCodeVerifyingAlgorithmEvalutor;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.VerifySATTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.prepostcommand.PrePostCommandTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.loader.TargetAlgorithmEvaluatorLoader;


public class TargetAlgorithmEvaluatorBuilder {

	private static Logger log = LoggerFactory.getLogger(TargetAlgorithmEvaluatorBuilder.class);
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * 
	 * @param options 		   Target Algorithm Evaluator Options
	 * @param execConfig	   Execution configuration for the target algorithm
	 * @param noHashVerifiers  Whether we should apply hash verifiers				
	 * @return
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options, AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed, Map<String, AbstractOptions> taeOptionsMap)
	{
		return getTargetAlgorithmEvaluator(options, execConfig, hashVerifiersAllowed, taeOptionsMap, null);
	}
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * 
	 * @param options 		   Target Algorithm Evaluator Options
	 * @param execConfig	   Execution configuration for the target algorithm
	 * @param noHashVerifiers  Whether we should apply hash verifiers
	 * @param tae			   Existing Target Algorithm Evaluator to wrap (if null, will use the options to construct one)				
	 * @return
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options, AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed, Map<String, AbstractOptions> taeOptionsMap, TargetAlgorithmEvaluator tae)
	{
		
		if(taeOptionsMap == null)
		{
			throw new IllegalArgumentException("taeOptionsMap must be non-null and contain the option objects for all target algorithm evaluators");
		}
		
	
		if(tae == null)
		{
			String taeKey = options.targetAlgorithmEvaluator;
			AbstractOptions taeOptions = taeOptionsMap.get(taeKey);
			tae = TargetAlgorithmEvaluatorLoader.getTargetAlgorithmEvaluator(execConfig, taeKey,taeOptions);
		} 
		
		if(tae == null)
		{
			throw new IllegalStateException("TAE should have been non-null");
		}
		//===== Note the decorators are not in general commutative
		//Specifically Run Hash codes should only see the same runs the rest of the applications see
		//Additionally retrying of crashed runs should probably happen before Abort on Crash
		
		if(options.retryCount >0)
		{
			log.debug("[TAE] Automatically retrying CRASHED runs {} times " , options.retryCount);
			tae = new RetryCrashedRunsTargetAlgorithmEvaluator(options.retryCount, tae);
		}
		
		
		
		if(options.abortOnCrash)
		{
			log.debug("[TAE] Treating all crashes as aborts");
			tae = new AbortOnCrashTargetAlgorithmEvaluator(tae);
		}
		
		
		if(options.abortOnFirstRunCrash)
		{
			tae = new AbortOnFirstRunCrashTargetAlgorithmEvaluator(tae);
			
			if(options.abortOnCrash)
			{
				log.warn("[TAE] Configured to treat all crashes as aborts, it is redundant to also treat the first as an abort");
			}
		}
		
		
		if(options.verifySAT != null)
		{
			if(options.verifySAT)
			{
				log.debug("[TAE] Verifying SAT Responses");
				tae = new VerifySATTargetAlgorithmEvaluator(tae);
				
			}
		}
		
		if(options.checkSATConsistency)
		{
			log.debug("[TAE] Ensuring SAT Response consistency");
			tae = new SATConsistencyTargetAlgorithmEvaluator(tae, options.checkSATConsistencyException);
		}
		
		if(options.boundRuns)
		{
			log.debug("[TAE] Bounding the number of concurrent target algorithm evaluations to {} ", options.maxConcurrentAlgoExecs);
			tae = new BoundedTargetAlgorithmEvaluator(tae, options.maxConcurrentAlgoExecs, execConfig);
		}
	

		//==== Run Hash Code Verification should generally be one of the last
		// things we add since it is very sensitive to the actual runs being run. (i.e. a retried run or a change in the run may change a hashCode in a way the logs don't reveal
		if(hashVerifiersAllowed)
		{
			
			if(options.leakMemory)
			{
				LeakingMemoryTargetAlgorithmEvaluator.leakMemoryAmount(options.leakMemoryAmount);
				log.warn("[TAE] Target Algorithm Evaluators will leak memory. I hope you know what you are doing");
				tae = new LeakingMemoryTargetAlgorithmEvaluator(tae);
				
			}
			
			if(options.runHashCodeFile != null)
			{
				log.info("[TAE] Algorithm Execution will verify run Hash Codes");
				Queue<Integer> runHashCodes = parseRunHashCodes(options.runHashCodeFile);
				tae = new RunHashCodeVerifyingAlgorithmEvalutor(tae, runHashCodes);
				 
			} else
			{
				log.info("[TAE] Algorithm Execution will NOT verify run Hash Codes");
				tae = new RunHashCodeVerifyingAlgorithmEvalutor(tae);
			}

		}
		
		//==== Doesn't change anything and so is safe after the RunHashCode
		tae = new TimingCheckerTargetAlgorithmEvaluator(execConfig, tae);
		
		//==== Doesn't change anything and so is safe after the RunHashCode
		tae = new PrePostCommandTargetAlgorithmEvaluator(tae, options.prePostOptions);
		return tae;
	}
	
	
	
	private static Pattern RUN_HASH_CODE_PATTERN = Pattern.compile("^Run Hash Codes:\\d+( After \\d+ runs)?\\z");
	
	private static Queue<Integer> parseRunHashCodes(File runHashCodeFile) 
	{
		log.info("Run Hash Code File Passed {}", runHashCodeFile.getAbsolutePath());
		Queue<Integer> runHashCodeQueue = new LinkedList<Integer>();
		BufferedReader bin = null;
		try {
			try{
				bin = new BufferedReader(new FileReader(runHashCodeFile));
			
				String line;
				int hashCodeCount=0;
				int lineCount = 1;
				while((line = bin.readLine()) != null)
				{
					
					Matcher m = RUN_HASH_CODE_PATTERN.matcher(line);
					if(m.find())
					{
						Object[] array = { ++hashCodeCount, lineCount, line};
						log.debug("Found Run Hash Code #{} on line #{} with contents:{}", array);
						int colonIndex = line.indexOf(":");
						int spaceIndex = line.indexOf(" ", colonIndex);
						String lineSubStr = line.substring(colonIndex+1,spaceIndex);
						runHashCodeQueue.add(Integer.valueOf(lineSubStr));
						
					} else
					{
						log.trace("No Hash Code found on line: {}", line );
					}
					lineCount++;
				}
				if(hashCodeCount == 0)
				{
					log.warn("Hash Code File Specified, but we found no hash codes");
				}
			
			} finally
			{
				if(bin != null) bin.close();
			}
			
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		
		
		return runHashCodeQueue;
		
	}
	
}
