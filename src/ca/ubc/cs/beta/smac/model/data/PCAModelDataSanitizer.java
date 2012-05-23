package ca.ubc.cs.beta.smac.model.data;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.configspace.ParamConfigurationSpace;
import ca.ubc.cs.beta.smac.PCA;
import ca.ubc.cs.beta.smac.PCA.Operation;
import ca.ubc.cs.beta.smac.helper.ArrayMathOps;

/**
 * This class roughly does all the processing for sanitizing data
 * @author seramage
 *
 */
public class PCAModelDataSanitizer extends AbstractSanitizedModelData {

	
	private final double[][] pcaVec;
	private final double[] pcaCoeff;
	private final int[] sub;
	private final double[] means;
	private final double[] stdDev;
	private final double[][] pcaFeatures;
	private final double[][] prePCAInstanceFeatures;
	private final double[] responseValues;
	private final ParamConfigurationSpace configSpace;
	private final double[][] configs;
	
	private final boolean logModel;
	/**
	 * Debugging crap that basically writes the arguments to a file that you can then use to test outside of Matlab
	 */
	public static int index = 0;
	public static final String filename = "/tmp/lastoutput-mds";
	static boolean writeOutput = true;
	private Logger log = LoggerFactory.getLogger(getClass());
	public static void main(String[] args)
	{
		/*
		double[][] m1 = {{ 1,2},{3,4},{5,6}};
		double[][] m2 = {{1,2,3},{4,5,6}};
		System.out.println(explode(Arrays.deepToString((new PCA()).matrixMultiply(m1, m2))));
		 */
		File f = new File(filename + "-" + 1);
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new FileInputStream(f));
		
		double[][] instanceFeatures  = (double[][]) in.readObject();
		double[][] paramValues = (double[][]) in.readObject();
		double[] responseValues = (double[]) in.readObject();
		in.close();
	
		writeOutput = false;
		
		int numPCA = 7;
		
		boolean logModel = true;
		
		
		SanitizedModelData mdc = new PCAModelDataSanitizer(instanceFeatures, paramValues, numPCA, responseValues, new int[1], logModel);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public static String explode(String s)
	{
		return s.replaceAll("]","}\n").replaceAll("\\[", "{");
	}
	
	public PCAModelDataSanitizer(double[][] instanceFeatures, double[][] paramValues, int numPCA, double[] responseValues, int[] usedInstances, boolean logModel)
	{
		this(instanceFeatures, paramValues, numPCA, responseValues, usedInstances, logModel, null);
	}
	public PCAModelDataSanitizer(double[][] instanceFeatures, double[][] paramValues, int numPCA, double[] responseValues, int[] usedInstancesIdxs, boolean logModel, ParamConfigurationSpace configSpace)
	{
		this.configSpace = configSpace;
		this.configs = paramValues;
		this.responseValues = responseValues;
		
		this.prePCAInstanceFeatures = ArrayMathOps.copy(instanceFeatures);
		
		instanceFeatures = ArrayMathOps.copy(instanceFeatures);
		writeOutput = false;
		if(writeOutput)
		{
			File f = new File(filename + "-" + index);
			f.delete();
			
			try { 
			ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(f));
			/*
			System.out.println("double[][] instanceFeatures = " + explode(Arrays.deepToString(instanceFeatures)) + ";");
			System.out.println("double[][] paramValues = " + explode(Arrays.deepToString(paramValues)) + ";");
			System.out.println("double[] responseValues = " + explode(Arrays.toString(responseValues)) + ";");
			*/
			o.writeObject(instanceFeatures);
			o.writeObject(paramValues);
			o.writeObject(responseValues);
			System.out.println("Calls written & deleted to: " + filename + "-" + index++ );
			o.close();
			} catch(IOException e)
			{
				System.err.println(e);
			}
		}
		
		PCA pca = new PCA();
		double[][] usedInstanceFeatures = new double[usedInstancesIdxs.length][];
		
		for(int i=0; i < usedInstanceFeatures.length; i++)
		{
			usedInstanceFeatures[i] = instanceFeatures[usedInstancesIdxs[i]];
		}
		int[] constFeatures = pca.constantColumnsWithMissingValues(usedInstanceFeatures);
		instanceFeatures = pca.removeColumns(instanceFeatures, constFeatures);
		
		log.info("Discarding {} constant inputs of {} in total.", constFeatures.length, prePCAInstanceFeatures[0].length);
		double[][] instanceFeaturesT = pca.transpose(instanceFeatures);
		
		
		double[] firstStdDev = pca.getRowStdDev(instanceFeaturesT);
		//double[][] pcaedFeatures =pca.getPCA(instanceFeatures, numPCA); 
		
		this.logModel = logModel;
		if(logModel)
		{
			pca.max(responseValues, SanitizedModelData.MINIMUM_RESPONSE_VALUE);
			pca.log10(responseValues);
		
		}
		
		//TODO: Give this variable an intellegent name
		sub = pca.getSub(firstStdDev);

		if(sub.length == 0)
		{
			//throw new IllegalStateException("Not sure what to do in this case at the moment");
			
			means = new double[0];
			stdDev = new double[0];
			pcaCoeff = new double[0];
			pcaVec = new double[0][];
			pcaFeatures = new double[instanceFeatures.length][1];
			
			return;
		
		}
		
		instanceFeatures = pca.keepColumns(instanceFeatures, sub);
		instanceFeaturesT = pca.transpose(instanceFeatures);
		means = pca.getRowMeans(instanceFeaturesT);
		stdDev = pca.getRowStdDev(instanceFeaturesT);
		
				
		pca.perColumnOperation(instanceFeatures, means, Operation.SUBTRACT);
		pca.perColumnOperation(instanceFeatures, stdDev, Operation.DIVIDE);
		
		pcaCoeff = pca.getPCACoeff(instanceFeatures, numPCA);
		pcaVec = pca.getPCA(instanceFeatures, numPCA);
		
		
		//double[][] pcaVecT = pca.transpose(pcaVec);
		pcaFeatures = pca.matrixMultiply(instanceFeatures, pcaVec);
		
		int[] constParams = pca.constantColumns(instanceFeatures);
		
		//paramValues = pca.removeConstantColumns(paramValues, constParams);
	}

	/* (non-Javadoc)
	 * @see ca.ubc.cs.beta.smac.model.SanitizedModelData#getPrePCAInstanceFeatures()
	 */
	@Override
	public double[][] getPrePCAInstanceFeatures()
	{
		return prePCAInstanceFeatures;
	}
	
	/* (non-Javadoc)
	 * @see ca.ubc.cs.beta.smac.model.SanitizedModelData#getPCAVectors()
	 */
	@Override
	public double[][] getPCAVectors() {
		return pcaVec;
	}

	/* (non-Javadoc)
	 * @see ca.ubc.cs.beta.smac.model.SanitizedModelData#getPCACoefficients()
	 */
	@Override
	public double[] getPCACoefficients() {
		return pcaCoeff;
	}

	/* (non-Javadoc)
	 * @see ca.ubc.cs.beta.smac.model.SanitizedModelData#getDataRichIndexes()
	 */
	@Override
	public int[] getDataRichIndexes() {
		return sub;
	}

	/* (non-Javadoc)
	 * @see ca.ubc.cs.beta.smac.model.SanitizedModelData#getMeans()
	 */
	@Override
	public double[] getMeans() {
		return means;
	}

	/* (non-Javadoc)
	 * @see ca.ubc.cs.beta.smac.model.SanitizedModelData#getStdDev()
	 */
	@Override
	public double[] getStdDev() {
		return stdDev;
	}
	
	/* (non-Javadoc)
	 * @see ca.ubc.cs.beta.smac.model.SanitizedModelData#getPCAFeatures()
	 */
	@Override
	public double[][] getPCAFeatures()
	{
		return pcaFeatures;
	}
	
	/**
	 * Return an array containing all the parameter configurations in array format
	 * @return
	 */
	public double[][] getConfigs()
	{
		return configs;
	}
	
	/**
	 * Returns the response values (transformed if necessary)
	 * @return
	 */
	public double[] getResponseValues()
	{
		return responseValues;
	}

	public int[] getCategoricalSize()
	{
		return configSpace.getCategoricalSize();
	}
	public int[][] getCondParents()
	{
		return configSpace.getCondParentsArray();
	}

	public int[][][] getCondParentVals()
	{
		return configSpace.getCondParentValsArray();
	}

	public double transformResponseValue(double d) {
		if(logModel)
		{
			
			return Math.log10(Math.max(d, SanitizedModelData.MINIMUM_RESPONSE_VALUE));
		} else
		{
			return d;
		}
	}
	
}
