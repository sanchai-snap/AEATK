package ca.ubc.cs.beta.aclib.misc.jcommander.converter;

import java.util.Arrays;

import ca.ubc.cs.beta.aclib.objectives.RunObjective;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class RunObjectiveConverter implements IStringConverter<RunObjective> {

	@Override
	public RunObjective convert(String arg0) {
		try {
			return RunObjective.valueOf(arg0.toUpperCase());
		} catch(IllegalArgumentException e)
		{
			throw new ParameterException("Illegal value specified for Run Objective ("  + arg0 + "), allowed values are: " + Arrays.toString(RunObjective.values()));
		}
	}

}
