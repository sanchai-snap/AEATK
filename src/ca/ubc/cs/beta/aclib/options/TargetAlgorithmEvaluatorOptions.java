package ca.ubc.cs.beta.aclib.options;

import java.io.File;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.NonNegativeInteger;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.ReadableFileConverter;
import ca.ubc.cs.beta.aclib.misc.jcommander.validator.TAEValidator;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.targetalgorithmevaluator.decorators.prepostcommand.PrePostCommandOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.validators.PositiveInteger;

@UsageTextField(title="Target Algorithm Evaluator Options", description="Options that describe and control the policy and mechanisms for algorithm execution")
public class TargetAlgorithmEvaluatorOptions extends AbstractOptions {
		
	
	//@UsageTextField(domain="")
	@Parameter(names={"--targetAlgorithmEvaluator","--tae"}, description="Target Algorithm Evaluator to use when making target algorithm calls", validateWith=TAEValidator.class)
	public String targetAlgorithmEvaluator = "CLI";
	
	@Parameter(names="--abortOnCrash", description="treat algorithm crashes as an ABORT (Useful if algorithm should never CRASH). NOTE:  This only aborts if all retries fail.")
	public boolean abortOnCrash = false;

	@Parameter(names="--abortOnFirstRunCrash", description="if the first run of the algorithm CRASHED treat it as an ABORT, otherwise allow crashes.")
	public boolean abortOnFirstRunCrash = true;

	@Parameter(names={"--retryCrashedRunCount","--retryTargetAlgorithmRunCount"}, description="number of times to retry an algorithm run before reporting crashed (NOTE: The original crashes DO NOT count towards any time limits, they are in effect lost). Additionally this only retries CRASHED runs, not ABORT runs, this is by design as ABORT is only for cases when we shouldn't bother further runs", validateWith=NonNegativeInteger.class)
	public int retryCount = 0;

	@Parameter(names={"--boundRuns"}, description="if true, permit only --cores number of runs to be evaluated concurrently")
	public boolean boundRuns = true;
	
	@Parameter(names={"--cores","--numConcurrentAlgoExecs","--maxConcurrentAlgoExecs","--numberOfConcurrentAlgoExecs"}, description="maximum number of concurrent target algorithm executions", validateWith=PositiveInteger.class)
	public int maxConcurrentAlgoExecs = 1;
	
	@UsageTextField(defaultValues="")
	@Parameter(names="--runHashCodeFile", description="file containing a list of run hashes one per line: Each line should be: \"Run Hash Codes: (Hash Code) After (n) runs\". The number of runs in this file need not match the number of runs that we execute, this file only ensures that the sequences never diverge. Note the n is completely ignored so the order they are specified in is the order we expect the hash codes in this version. Finally note you can simply point this at a previous log and other lines will be disregarded", converter=ReadableFileConverter.class)
	public File runHashCodeFile;

	@Parameter(names="--leakMemoryAmount", hidden=true, description="amount of memory in bytes to leak")
	public int leakMemoryAmount = 1024;

	@Parameter(names="--leakMemory", hidden=true, description="leaks some amount of memory for every run")
	public boolean leakMemory = false;
	
	@Parameter(names="--verifySAT", description="Check SAT/UNSAT/UNKNOWN responses against Instance specific information (if null then performs check if every instance has specific information in the following domain {SAT, UNSAT, UNKNOWN, SATISFIABLE, UNSATISFIABLE}")
	public Boolean verifySAT;

	@Parameter(names="--checkSATConsistency", description="Ensure that runs on the same problem instance always return the same SAT/UNSAT result")
	public boolean checkSATConsistency = false;

	@Parameter(names="--checkSATConsistencyException", description="Throw an exception if runs on the same problem instance disagree with respect to SAT/UNSAT")
	public boolean checkSATConsistencyException = false;
	
	@ParametersDelegate
	public PrePostCommandOptions prePostOptions = new PrePostCommandOptions();

	@Parameter(names="--checkResultOrderConsistent", description="Check that the TAE is returning responses in the correct order")
	public boolean checkResultOrderConsistent;
	
	
	
}
