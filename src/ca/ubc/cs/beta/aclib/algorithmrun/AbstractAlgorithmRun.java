package ca.ubc.cs.beta.aclib.algorithmrun;

import ca.ubc.cs.beta.aclib.execconfig.AlgorithmExecutionConfig;
import ca.ubc.cs.beta.aclib.runconfig.RunConfig;

/**
 * This class represents a single run of the target algorithm given by the AlgorithmExecutionConfig object and the RunConfig object
 * 
 * @author seramage
 *
 */
public abstract class AbstractAlgorithmRun implements Runnable, AlgorithmRun{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1860615761848618478L;
	
	
	protected final RunConfig runConfig;
	protected final AlgorithmExecutionConfig execConfig;
	
	/*
	 * Values reported by the target algorithm
	 */
	private RunResult acResult;
	
	private double runtime;
	private double runLength;
	private double quality;
	private  long resultSeed; 
	
	/**
	 * Result line reported by the target algorithm (for debug purposes only), 
	 * NOTE: This will always be parsable, and may not be what the algorithm reported
	 */
	private String resultLine;
	
	/**
	 * Raw result line reported by the target algorithm (potentially useful if the result line is corrupt)
	 */
	private String rawResultLine;
	
	/**
	 * true if the run is completed 
	 */
	private boolean runCompleted = false;
	
	/**
	 * True if the run was well formed
	 * Note: We may deprecate this in favor of using CRASHED
	 */
	private boolean runResultWellFormed = false;
	
	
	/**
	 * Sets the values for this Algorithm Run
	 * @param acResult
	 * @param runtime
	 * @param runLength
	 * @param quality
	 * @param resultSeed
	 * @param rawResultLine
	 */
	protected synchronized void setResult(RunResult acResult, double runtime, double runLength, double quality, long resultSeed, String rawResultLine)
	{
		
		this.acResult = acResult;
		this.runtime = runtime;
		this.runLength = runLength;
		this.quality = quality;
		this.resultSeed = resultSeed;
		this.resultLine =  acResult.name() + ", " + runtime + ", " + runLength + ", " + quality + ", " + resultSeed;
		this.rawResultLine = rawResultLine;
		this.runResultWellFormed = true;
		this.runCompleted = true;
	}
	
	/**
	 * Marks this run as aborted
	 * @param rawResultLine  the raw output that might be relevant to this abort
	 */
	protected void setAbortResult(String rawResultLine)
	{
		this.setResult(RunResult.ABORT, runConfig.getCutoffTime(), 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), rawResultLine);
	}
	
	/**
	 * Marks this run as aborted
	 * @param rawResultLine  the raw output that might be relevant to this crash
	 */
	protected void setCrashResult(String rawResultLine)
	{
		this.setResult(RunResult.CRASHED, runConfig.getCutoffTime(), 0, 0, runConfig.getProblemInstanceSeedPair().getSeed(), rawResultLine);
	}
	
	
	
	/**
	 * Sets the values for this Algorithm Run
	 * @param acResult					RunResult for this run
	 * @param runtime					runtime measured
	 * @param runLength					runlength measured
	 * @param quality					quality measured
	 * @param resultSeed				resultSeed 
	 * @param resultLine				well formatted result line 
	 * @param rawResultLine				raw result line
	 * @param runResultWellFormed		whether this run has well formed output
	 */
	protected synchronized void setResult(RunResult acResult, double runtime, double runLength, double quality, long resultSeed, String resultLine, String rawResultLine, boolean runResultWellFormed)
	{
		this.acResult = acResult;
		this.runtime = runtime;
		this.runLength = runLength;
		this.quality = quality;
		this.resultSeed = resultSeed;
		this.resultLine = resultLine;
		this.rawResultLine = rawResultLine;
		this.runResultWellFormed = runResultWellFormed;
		this.runCompleted = true;
	}
	
	
	/**
	 * Default Constructor
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 */
	public AbstractAlgorithmRun(AlgorithmExecutionConfig execConfig, RunConfig runConfig)
	{
		if(execConfig == null || runConfig == null)
		{
			throw new IllegalArgumentException("Arguments cannot be null");
		}
		
		this.runConfig = runConfig;
		this.execConfig = execConfig;
	}
	
	@Override
	public abstract void run();

	/**
	 * Synonym of {@link AbstractAlgorithmRun#run()}
	 * <p>
	 * <b>Implementation Note:</b> If there is a good reason this method can be made non final 
	 * but as a rule call should be the same a run().
	 * 
	 *  @return null
	 */
	@Override
	public final Object call()
	{
		run();
		return null;
	}
	
	@Override
	public final AlgorithmExecutionConfig getExecutionConfig()
	{
		return execConfig;
	}
	
	@Override
	public final RunConfig getRunConfig()
	{
		return runConfig;
	}
	
	
	@Override
	public final RunResult getRunResult() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return acResult;
	}

	@Override
	public final double getRuntime() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return runtime;
	}

	@Override
	public final double getRunLength() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return runLength;
	}

	@Override
	public final double getQuality() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return quality;
	}

	@Override
	public final long getResultSeed() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return resultSeed;
	}

	@Override
	public final String getResultLine() {
		if(!isRunResultWellFormed()) throw new IllegalStateException("Execution Result was not well formed");
		return resultLine;
	}

	@Override
	public final synchronized boolean isRunCompleted() {
		return runCompleted;
	}

	@Override
	public final synchronized boolean isRunResultWellFormed() {
		if(!isRunCompleted()) throw new IllegalStateException("Run has not yet completed");
		return runResultWellFormed;
	}
	
	@Override
	public final String rawResultLine()
	{
		if(!isRunCompleted()) throw new IllegalStateException("Run has not yet completed");
		return rawResultLine;
	}
	
	@Override
	public int hashCode()
	{
		//I believe that just instanceConfig and not execConfig hashCodes should be good enough
		//Since it's rare for two different execConfigs to have identical instanceConfigs
		return runConfig.hashCode();
	}
	
	/**
	 * Two AlgorithmRuns are considered equal if they have same runConfig and execConfig
	 */
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof AbstractAlgorithmRun)
		{
			AbstractAlgorithmRun aro = (AbstractAlgorithmRun) o;
			return aro.execConfig.equals(execConfig) && aro.runConfig.equals(runConfig);
		} 
		return false;
	}
	
	@Override 
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(execConfig.toString()).append("\n");
		sb.append(runConfig.toString());
		sb.append("\nResult Line:" + resultLine) ;
		sb.append("\nRawResultLine:" + rawResultLine);
		sb.append("\nrunCompleted:" + runCompleted);
		sb.append("\nacResult:" + acResult);
		sb.append("\nClass:" + this.getClass().getSimpleName());
		return sb.toString();
		
		
	}
	
	
	
	
}