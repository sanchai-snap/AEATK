package ca.ubc.cs.beta.aclib.example.statemerge;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aclib.help.HelpOptions;
import ca.ubc.cs.beta.aclib.misc.options.UsageTextField;
import ca.ubc.cs.beta.aclib.options.AbstractOptions;
import ca.ubc.cs.beta.aclib.options.scenario.ScenarioOptions;

@UsageTextField(title="State File Merge Utility", description="Merges many different state files", noarg=StateMergeNoArgumentHandler.class)
public class StateMergeOptions extends AbstractOptions {

	@ParametersDelegate
	ScenarioOptions scenOpts = new ScenarioOptions();
	
	@Parameter(names="--directories", description="Directories to search for state files", variableArity = true)
	public List<String> directories;
	
	@Parameter(names="--up-to-iteration", description="Only restore runs up to iteration ")
	public int iterationLimit = Integer.MAX_VALUE;
	
	@Parameter(names="--up-to-tunertime", description="Only restore runs up to tuner time limit")
	public int tunerTime = Integer.MAX_VALUE;
	
	
	@ParametersDelegate 
	public HelpOptions helpOptions = new HelpOptions();
	
}
