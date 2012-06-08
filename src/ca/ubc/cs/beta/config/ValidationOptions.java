package ca.ubc.cs.beta.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;

public class ValidationOptions {

	@Parameter(names="--numSeedsPerTestInstance", description="Number of test seeds to use per instance during validation", validateWith=PositiveInteger.class)
	public int numberOfTestSeedsPerInstance = 1000;
	
	@Parameter(names="--numTestInstances", description = "Number of instances to test against (Will execute min of this, and number of instances in test Instance File)")
	public int numberOfTestInstances = Integer.MAX_VALUE;

	@Parameter(names="--numberOfValidationRuns", description = "Approximate Number of Validation Runs to do (SMAC will always make sure to do the same number of seeds per instance, and so will round this number")
	public int numberOfValidationRuns = 1000;
	
	@Parameter(names="--validationRoundingMode", description="Whether to round the number of validation runs up or down (to next multiple of numTestInstances")
	public ValidationRoundingMode validationRoundingMode = ValidationRoundingMode.UP;

	@Parameter(names="--noValidationHeaders", description="Don't put headers on output CSV files for Validation")
	public boolean noValidationHeaders = false;
	
}
