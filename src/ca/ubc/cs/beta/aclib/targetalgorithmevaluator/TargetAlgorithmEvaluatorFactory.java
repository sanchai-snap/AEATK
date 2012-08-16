package ca.ubc.cs.beta.aclib.targetalgorithmevaluator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.options.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.aclib.options.ScenarioOptions;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbortOnCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.AbortOnFirstRunCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.RetryCrashedRunsTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.RunHashCodeVerifyingAlgorithmEvalutor;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.loader.TargetAlgorithmEvaluatorLoader;


public class TargetAlgorithmEvaluatorFactory {

	private static Logger log = LoggerFactory.getLogger(TargetAlgorithmEvaluatorFactory.class);

	
	public static List<String> getAvailableTargetAlgorithmEvaluators(AlgorithmExecutionOptions config)
	{
		ClassLoader cl = getClassLoader(config);
		
		return TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators(cl);
		
	}
	/**
	 * Retrieves a modified class loader to do dynamically search for jars
	 * @return
	 */
	private static ClassLoader getClassLoader(AlgorithmExecutionOptions options)
	{
		String pathtoSearch = options.taeSearchPath;
		String[] paths = pathtoSearch.split(File.pathSeparator);
		
		ArrayList<URL> urls = new ArrayList<URL>(paths.length);
				
		for(String path : paths)
		{
			
			File f = new File(path);
			
			try {
				urls.add(f.toURI().toURL());
				
			} catch (MalformedURLException e) {
				log.info("Could not parse path {}, got {}", path, e );
			}
			
			
		}
		
		
		URL[] urlsArr = urls.toArray(new URL[0]);
		
		
		URLClassLoader ucl = new URLClassLoader(urlsArr);
		
		return ucl;
		
		
		
	}
	
	
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(ScenarioOptions scenarioOptions, AlgorithmExecutionConfig execConfig)
	{
		return getTargetAlgorithmEvaluator(scenarioOptions, execConfig, true);
	}
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * @param options
	 * @param execConfig
	 * @param noHashVerifiers
	 * @return
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(ScenarioOptions options, AlgorithmExecutionConfig execConfig, boolean hashVerifiersAllowed)
	{
		
		ClassLoader cl = getClassLoader(options.algoExecOptions);
		//TargetAlgorithmEvaluator cli = TargetAlgorithmEvaluatorLoader.getTargetAlgorithmEvaluator(execConfig, options.maxConcurrentAlgoExecs, "CLI",cl);
		//TargetAlgorithmEvaluator surrogate = TargetAlgorithmEvaluatorLoader.getTargetAlgorithmEvaluator(execConfig, options.maxConcurrentAlgoExecs, options.scenarioConfig.algoExecOptions.targetAlgorithmEvaluator,cl);
		
		 
		TargetAlgorithmEvaluator algoEval = TargetAlgorithmEvaluatorLoader.getTargetAlgorithmEvaluator(execConfig, options.algoExecOptions.maxConcurrentAlgoExecs, options.algoExecOptions.targetAlgorithmEvaluator,cl);
		
		//===== Note the decorators are not in general commutative
		//Specifically Run Hash codes should only see the same runs the rest of the applications see
		//Additionally retrying of crashed runs should probably happen before Abort on Crash
		
		algoEval = new RetryCrashedRunsTargetAlgorithmEvaluator(options.algoExecOptions.retryCount, algoEval);
		
		
		if(options.algoExecOptions.abortOnCrash)
		{
			algoEval = new AbortOnCrashTargetAlgorithmEvaluator(algoEval);
		}
		
		
		if(options.algoExecOptions.abortOnFirstRunCrash)
		{
			algoEval = new AbortOnFirstRunCrashTargetAlgorithmEvaluator(algoEval);
			
			if(options.algoExecOptions.abortOnCrash)
			{
				log.warn("Configured to treat all crashes as aborts, it is redundant to also treat the first as an abort");
			}
		}
		
		//==== Run Hash Code Verification should be last
		if(hashVerifiersAllowed)
		{
			if(options.algoExecOptions.runHashCodeFile != null)
			{
				log.info("Algorithm Execution will verify run Hash Codes");
				Queue<Integer> runHashCodes = parseRunHashCodes(options.algoExecOptions.runHashCodeFile);
				algoEval = new RunHashCodeVerifyingAlgorithmEvalutor(algoEval, runHashCodes);
				 
			} else
			{
				log.info("Algorithm Execution will NOT verify run Hash Codes");
				algoEval = new RunHashCodeVerifyingAlgorithmEvalutor(algoEval);
			}

		}
		
		return algoEval;
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