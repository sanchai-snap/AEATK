package ca.ubc.cs.beta.probleminstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.ac.config.ProblemInstance;
import ca.ubc.cs.beta.models.surrogate.helpers.csv.ConfigCSVFileHelper;
import ca.ubc.cs.beta.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.seedgenerator.RandomInstanceSeedGenerator;
import ca.ubc.cs.beta.seedgenerator.SetInstanceSeedGenerator;

import com.beust.jcommander.ParameterException;

public class ProblemInstanceHelper {
	
	private static File getFileForPath(String context, String path)
	{
		File f;
		logger.trace("Trying to find file with context {} and path {}", context, path);
		if(path.substring(0, 1).equals(File.separator))
		{
			logger.trace("Absolute path given for path, checking {}", path);
			f = new File(path);
		} else
		{
			Object[] args = { context, File.separator, path };
			logger.trace("Relative path given for path, checking {}{}{}", args);
			f = new File(context + File.separator + path);
		}
		
		if(!f.exists())
		{
			throw new ParameterException("Could not find needed file:" + path + " Context:" + context);
		}
		
		return f;
	}
	

	private static Logger logger = LoggerFactory.getLogger(ProblemInstanceHelper.class);
	
	
	
	private static final Map<String, ProblemInstance> cachedProblemInstances = new HashMap<String, ProblemInstance>();

	
	public static void clearCache()
	{
		cachedProblemInstances.clear();
		
	}
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, boolean checkFileExistsOnDisk) throws IOException
	{
		return getInstances(filename, experimentDir, null, checkFileExistsOnDisk);
	}
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, boolean checkFileExistsOnDisk, boolean deterministic) throws IOException
	{
		return getInstances(filename, experimentDir, null, checkFileExistsOnDisk, deterministic);
	}
	
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk) throws IOException	{
	
		return getInstances(filename, experimentDir, featureFileName, checkFileExistsOnDisk, 0, Integer.MAX_VALUE);
	}
	
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk, long seed, boolean deterministic) throws IOException
	{
		return getInstances(filename, experimentDir, featureFileName, checkFileExistsOnDisk, seed, Integer.MAX_VALUE, deterministic);
	}
	
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk, long seed, int maxSeedsPerInstance) throws IOException
	{
		
		return getInstances(filename, experimentDir, featureFileName, checkFileExistsOnDisk, seed, Integer.MAX_VALUE, false);
	}
	
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk, boolean deterministic) throws IOException	{
		
		return getInstances(filename, experimentDir, featureFileName, checkFileExistsOnDisk, 0, Integer.MAX_VALUE, deterministic);
	}
	
	
	public static InstanceListWithSeeds getInstances(String instanceFileName, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk, long seed, int maxSeedsPerInstance, boolean deterministic) throws IOException {
		
		logger.info("Loading instances from file: {} and experiment dir {}", instanceFileName, experimentDir);
		

		List<ProblemInstance> instances = new ArrayList<ProblemInstance>();
		Set<ProblemInstance> instancesSet = new HashSet<ProblemInstance>();
		
		
		
		String line = "";
		
		
		int instID=1; 
		
		
		
		Map<String, Map<String, Double>> featuresMap = new LinkedHashMap<String, Map<String, Double>>();
		
		int numberOfFeatures = 0;
		if(featureFileName != null)
		{
			logger.info("Feature File specified reading features from: {} ", featureFileName);
			File featureFile = getFileForPath(experimentDir, featureFileName);
			
			if(!featureFile.exists())
			{
				throw new ParameterException("Feature file given does not exist " + featureFile);
			}
			CSVReader featureCSV = new CSVReader(new FileReader(featureFile));
			ConfigCSVFileHelper features = new ConfigCSVFileHelper(featureCSV.readAll(),1,1);
			
			numberOfFeatures = features.getNumberOfDataColumns();
			logger.info("Feature File specifies: {} features for {} instances", numberOfFeatures, features.getNumberOfDataRows() );
			
			
			for(int i=0; i  < features.getNumberOfDataRows(); i++)
			{
				TreeMap<String, Double> instFeatMap = new TreeMap<String, Double>();
				
				featuresMap.put(features.getKeyForDataRow(i).replaceAll("//", "/"), Collections.unmodifiableMap(instFeatMap));
				
				for (int j=0; j < features.getNumberOfDataColumns(); j++)
				{		
						String key = features.getDataKeyByIndex(j);
						Double value = features.getDoubleDataValue(i, j);
						instFeatMap.put(key, value);
				}
			}
		}
		
		
		
		List<String> instanceList = new ArrayList<String>(featuresMap.size());
		InstanceSeedGenerator gen; 
		Map<String, String> instanceSpecificInfo = Collections.emptyMap();
		if(instanceFileName != null)
		{
			
			
			File instanceListFile = getFileForPath(experimentDir, instanceFileName);
			
			InstanceListWithSeeds insc = getListAndSeedGen(instanceListFile,seed, maxSeedsPerInstance);
			instanceList = insc.getInstancesByName();
			gen = insc.getSeedGen();
			instanceSpecificInfo = insc.getInstanceSpecificInfo();
			if(deterministic)
			{
				if(gen instanceof SetInstanceSeedGenerator)
				{
					logger.warn("Detected that seeds have been preloaded, yet the algorithm is listed as deterministic, generally this means we should use -1 as a seed");
					
					
				} else
				{
					logger.info("Deterministic Algorithm, selecting hard coded instance seed generator");
					
					
					LinkedHashMap<String, List<Long>> instanceSeedMap = new LinkedHashMap<String, List<Long>>(); 
					
					for(String i : instanceList)
					{
						List<Long> l = new ArrayList<Long>(1);
						l.add(-1L);
						
						instanceSeedMap.put(i,l);
					}
					gen = new SetInstanceSeedGenerator(instanceSeedMap, instanceList, 1);
				}
				
			}
			
			
		} else
		{
			instanceList.addAll(featuresMap.keySet());
			gen = new RandomInstanceSeedGenerator(instanceList.size(), seed, maxSeedsPerInstance);
			
		}
		
		//Stores a set of features names that we haven't mapped to an instance 
		Set<String> unMappedFeatures = new HashSet<String>();
		unMappedFeatures.addAll(featuresMap.keySet());
		
		
		for(String instanceFile : instanceList)
		{
			
			
			if(checkFileExistsOnDisk)
			{
				File f = getFileForPath(experimentDir, instanceFile);
				
				//Should store the absolute file name if the file exists on disk
				//If we don't check if the file exists on disks we don't know whether to add experimentDir to it
				//This is primarily used for Surrogates
				
				instanceFile = f.getAbsolutePath();
				
				if(!f.exists())
				{
					
					throw new ParameterException("Instance does not exist on disk "+ f.getAbsolutePath());
					//logger.warn("Instance {} does not exist on disk", f.getAbsolutePath());
				}
			}
			Map<String, Double> features;
			
			
			if(featureFileName != null)
			{
				
				String[] possibleFiles = { instanceFile, instanceFile.replace(experimentDir, ""), instanceFile.replaceAll("//", "/"), instanceFile.replace(experimentDir, "").replaceAll("//","/")};
				
				features = null;
				for(String possibleFile : possibleFiles)
				{
					features = featuresMap.get(possibleFile.trim());
					
					if(features != null) 
					{
						logger.debug("Matched Features for file name : {}",possibleFile);
						unMappedFeatures.remove(possibleFile.trim());
						
						break;
					} else
					{
						logger.debug("No features found for file name : {}",possibleFile);
					}
					
				}
				/*
				if(features == null)
				{
					String path = instanceFile.replace(experimentDir,"");
					features = featuresMap.get(path);
				}*/
				
				
				
				
				if(features == null)
				{
					logger.warn("Could not find features for instance {} trying more creative matching, may be error prone and slow [probably not really]", instanceFile);
					
					for(Entry<String, Map<String, Double>> e : featuresMap.entrySet())						
					{
						if(instanceFile.endsWith(e.getKey()))
						{
							logger.info("Matched instance {} with this entry {}", instanceFile, e.getKey());
							features = e.getValue();
							break;
						} else
						{
							logger.trace("Didn't match ({}) with ({})", instanceFile, e.getKey());
						}
					}
					
				}
				if(features == null)
				{
					throw new ParameterException("Feature file : " + featureFileName + " does not contain feature data for instance: " + instanceFile);
				}
				
				
				if(features.size() != numberOfFeatures)
				{
					throw new ParameterException("Feature file : " + featureFileName + " contains " + features.size() + " for instance: " + instanceFile +  " but expected " + numberOfFeatures );
				}
			} else
			{
				features = Collections.emptyMap();
			}
			
			//Removes // from filenames as some files had this for some reason
			//We don't use Absolute File Name, because they may not actually exist
			instanceFile = instanceFile.replaceAll("//", "/");
			ProblemInstance ai;
			
			if(cachedProblemInstances.containsKey(instanceFile))
			{
				
				logger.trace("Instance file has already been loaded once this runtime, using cached instance of {}", instanceFile);
				ai = cachedProblemInstances.get(instanceFile);
				
				if(ai.getFeatures().size() > 0 && features.size() > 0)
				{
					if(!ai.getFeatures().equals(features))
					{
						logger.warn("We previously loaded an instance for filename {} but the instance Features don't match", instanceFile);
					}
				}
				
				
			} else
			{
				Map<String, String> fixedInstanceSpecificInfo = new HashMap<String, String>();
				for(Entry<String, String> ent : instanceSpecificInfo.entrySet())
				{
					fixedInstanceSpecificInfo.put(ent.getKey().replaceAll("//", "/"), ent.getValue());
				}
				
				ai = new ProblemInstance(instanceFile, instID++, features, fixedInstanceSpecificInfo.get(instanceFile));
				cachedProblemInstances.put(instanceFile, ai);
			}
			
			

			if(instancesSet.contains(ai))
			{
				logger.warn("Instance file seems to contain duplicate entries for the following filename {}", line);
			}
			
			instances.add(ai);
			instancesSet.add(ai);
			
		}
		
		//In typical use case we will have 50% test and 50% training set.
		//So this array will be twice the size
		List<ProblemInstance> instancesFromFeatures = new ArrayList<ProblemInstance>(instances.size() * 2);
		
		instancesFromFeatures.addAll(instances);
		
		for(String instanceFromFeatureFile : unMappedFeatures)
		{
			instancesFromFeatures.add(new ProblemInstance(instanceFromFeatureFile, instID++, featuresMap.get(instanceFromFeatureFile)));
		}
		
	
		logger.info("Found Instances loaded");
		return new InstanceListWithSeeds(gen, instances, instancesFromFeatures);
		
		
	}

	enum InstanceFileFormat
	{
		NEW_CSV_SEED_INSTANCE_PER_ROW,
		NEW_CSV_INSTANCE_PER_ROW,
		NEW_INSTANCE_SPECIFIC_PER_ROW,
		NEW_SEED_INSTANCE_SPECIFIC_PER_ROW,
		LEGACY_INSTANCE_PER_ROW,
		LEGACY_SEED_INSTANCE_PER_ROW, 
		LEGACY_INSTANCE_SPECIFIC_PER_ROW,
		LEGACY_SEED_INSTANCE_SPECIFIC_PER_ROW
	}
	private static InstanceListWithSeeds getListAndSeedGen(File instanceListFile, long seed, int maxSeedsPerConfig) throws IOException {
		
		String line;
		BufferedReader br = null;
		List<String> instanceList = new LinkedList<String>();
		
		
		logger.debug("Reading instance file detecting format");
		
		LinkedHashMap<String, List<Long>> instances;
		LinkedHashMap<String, String> instanceSpecificInfo;
		List<String> declaredInstanceOrderForSeeds = null;
		try
		{
			CSVReader reader = new CSVReader(new FileReader(instanceListFile),',','"',true);
			List<String[]> csvContents = reader.readAll();
			ValueObject v = parseCSVContents(csvContents, InstanceFileFormat.NEW_CSV_INSTANCE_PER_ROW, InstanceFileFormat.NEW_CSV_SEED_INSTANCE_PER_ROW, InstanceFileFormat.NEW_INSTANCE_SPECIFIC_PER_ROW, InstanceFileFormat.NEW_SEED_INSTANCE_SPECIFIC_PER_ROW);
			instances = v.instanceSeedMap;
			instanceSpecificInfo = v.instanceSpecificInfoMap;
			declaredInstanceOrderForSeeds = v.declaredInstanceOrderForSeeds;
		} catch(IllegalArgumentException e)
		{
			try { 
			
			/**
			 * For the old format we trim each line to get rid of spurious whitespace
			 */
			BufferedReader bufferedReader = new BufferedReader(new FileReader(instanceListFile));
			StringBuilder sb = new StringBuilder();
			while((line = bufferedReader.readLine()) != null)
			{
				sb.append(line.trim()).append("\n");
			}
			
				
			CSVReader reader = new CSVReader(new StringReader(sb.toString().trim()),' ');
			List<String[]> csvContents = reader.readAll();
			ValueObject v = parseCSVContents(csvContents, InstanceFileFormat.LEGACY_INSTANCE_PER_ROW, InstanceFileFormat.LEGACY_SEED_INSTANCE_PER_ROW, InstanceFileFormat.LEGACY_INSTANCE_SPECIFIC_PER_ROW, InstanceFileFormat.LEGACY_SEED_INSTANCE_SPECIFIC_PER_ROW);
			instances = v.instanceSeedMap;
			instanceSpecificInfo = v.instanceSpecificInfoMap;
			declaredInstanceOrderForSeeds = v.declaredInstanceOrderForSeeds;
					
			} catch(IllegalArgumentException e2)
			{
				throw new ParameterException("Could not parse instanceFile " + instanceListFile.getAbsolutePath());
			}
		}
		InstanceSeedGenerator gen;
		//We check if some entry has a non zero amount of seeds (if we are in an instance seed pair file all entries must have atleast one)
		//Then we use our manual instance seed generator
		if(instances.entrySet().iterator().next().getValue().size() > 0)
		{
			if(declaredInstanceOrderForSeeds == null)
			{
				throw new IllegalStateException("Expected instanceOrder to be specified, got null.");
			}
			gen = new SetInstanceSeedGenerator(instances,declaredInstanceOrderForSeeds, maxSeedsPerConfig);
		} else
		{
			gen = new RandomInstanceSeedGenerator(instances.size(),seed, maxSeedsPerConfig);
		}
		
		/*
		try
		{
			br = new BufferedReader(new FileReader(instanceListFile));
			while((line = br.readLine()) != null)
			{
				logger.trace("Read in line from file \"{}\"",line);
				instanceList.add(line);
				
			}
		} finally
		{
			if(br != null) br.close();
		}
		*/
		instanceList.addAll(instances.keySet());
		return new InstanceListWithSeeds(gen, null, instanceList, instanceSpecificInfo);
	}
	
	static class ValueObject
	{
		public List<String> declaredInstanceOrderForSeeds;
		public LinkedHashMap<String, List<Long>> instanceSeedMap;
		public LinkedHashMap<String, String> instanceSpecificInfoMap;
	}
	
	private static ValueObject parseCSVContents(List<String[]> csvContents, InstanceFileFormat instanceOnly, InstanceFileFormat seedPair, InstanceFileFormat instanceSpecific, InstanceFileFormat instanceSpecificSeed )
	{
		InstanceFileFormat possibleFormat = null;
	
		/**
		 * Note we make the determination of which instanceSeedGenerator to use based on the first entries list size()
		 */
		LinkedHashMap<String, List<Long>> instanceSeedMap = new LinkedHashMap<String, List<Long>>();
		
		/**
		 * Note we make the determination of which instanceSeedGenerator to use based on the first entries list size()
		 */
		LinkedHashMap<String, String> instanceSpecificInfoMap = new LinkedHashMap<String, String>();
		
		List<String> problemInstanceDeclaredOrder = new ArrayList<String>();
		
		for(String[] s : csvContents)
		{
			
			if(s.length == 1)
			{
				if(s[0].trim().equals("")) throw new IllegalArgumentException();
				
				if(possibleFormat == null)
				{
					possibleFormat = instanceOnly;
					logger.debug("Line with only 1 entry found, trying {}", possibleFormat);
				}
				if(possibleFormat == instanceOnly)
				{
					
					instanceSeedMap.put(s[0], new LinkedList<Long>());
				} else
				{
					logger.debug("Line with only 1 entry found, we are not {}",possibleFormat);
					throw new IllegalArgumentException();
				}
			} else if(s.length == 2)
			{
			
				if(possibleFormat == null)
				{
					try {
						possibleFormat = seedPair;
						logger.debug("Line with only 2 entries found, trying {}", possibleFormat);
						Long.valueOf(s[0]);
						possibleFormat = seedPair;
					} catch(NumberFormatException e)
					{
						possibleFormat = instanceSpecific;
						logger.debug("First entry on line 1 not a long value, trying {}", possibleFormat);
					}
					
					
				}
				
				
				if(possibleFormat.equals(seedPair))
				{
					String instanceName = s[1];
					try {
						if(instanceSeedMap.get(instanceName) == null)
						{
							instanceSeedMap.put(instanceName, new LinkedList<Long>());
						}
						
					
					instanceSeedMap.get(instanceName).add(Long.valueOf(s[0]));
					} catch(NumberFormatException e)
					{
						logger.debug("{} is not a valid long value", s[0]);
						
						throw new IllegalArgumentException();
					}
					
					problemInstanceDeclaredOrder.add(instanceName);
				} else if(possibleFormat.equals(instanceSpecific))
				{
					String instanceName = s[0];
					String instanceSpecificInfo = s[1];
					
					instanceSpecificInfoMap.put(instanceName, instanceSpecificInfo);
					instanceSeedMap.put(instanceName, new LinkedList<Long>());
				} else
				{
					logger.debug("Line with 2 entries found, we are not {}",possibleFormat);
					throw new IllegalArgumentException();
				}
			
			} else if(s.length == 3)
			{
				if(possibleFormat == null)
				{
					possibleFormat = instanceSpecificSeed;
				}
			
				if(possibleFormat == instanceSpecificSeed)
				{
					
					String instanceName = s[1];
					if(s[1].trim().length() == 0)
					{
						logger.debug("\"{}\" is not a valid instance name (All Whitespace)", s[1]);
						throw new IllegalArgumentException();
					}
					
					if(instanceSeedMap.get(instanceName) == null)
					{
						instanceSeedMap.put(instanceName, new LinkedList<Long>());
					}
					
					try
					{
					instanceSeedMap.get(instanceName).add(Long.valueOf(s[0]));
					
					} catch(NumberFormatException e)
					{
						logger.debug("{} is not a valid long value", s[0]);
						
						throw new IllegalArgumentException();
					}
					
					s[2] = s[2].trim();
					if(instanceSpecificInfoMap.get(instanceName) != null)
					{
						if(!s[2].equals(instanceSpecificInfoMap.get(instanceName)))
						{
							Object[] args = {instanceName, s[2], instanceSpecificInfoMap.get(instanceName)};
							logger.debug("Discrepancy detected in instance specific information {} had {} vs. {}  (This is not permitted)", args );
							throw new IllegalArgumentException();
						}
					} else
					{
						instanceSpecificInfoMap.put(instanceName, s[2]);
					}
					
					problemInstanceDeclaredOrder.add(instanceName);
					
				} else
				{
					logger.debug("Line with 3 entries found, we are not {}", possibleFormat);
					throw new IllegalArgumentException();
				}
				
			} else
			{
				logger.debug("Line with {} entries found unknown format", s.length);
				possibleFormat = null;
				throw new IllegalArgumentException();
			}
	}
			if(instanceSeedMap.size() == 0) throw new IllegalArgumentException("No Instances Found");
			ValueObject v = new ValueObject();
			v.instanceSeedMap = instanceSeedMap;
			v.instanceSpecificInfoMap = instanceSpecificInfoMap;
			v.declaredInstanceOrderForSeeds = problemInstanceDeclaredOrder;
			return v;
	}
	
}
