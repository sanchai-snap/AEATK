package ca.ubc.cs.beta.seedgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import ca.ubc.cs.beta.probleminstance.ProblemInstance;

public class SetInstanceSeedGenerator implements InstanceSeedGenerator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8634557199869232040L;
	private final LinkedHashMap<String, List<Long>> instances;
	private final int maxSeedsPerConfig;
	
	private final Map<String, Integer> piKeyToIntMap = new HashMap<String, Integer>();
	private final List<Queue<Long>> seeds;
	private final List<String> instanceOrder;
	private final int initialSeedCount;
	private final boolean allInstanceHaveSameNumberOfSeeds;
	
	
/*
	public SetInstanceSeedGenerator(
			LinkedHashMap<String, List<Long>> instances, int maxSeedsPerConfig) {
		if(maxSeedsPerConfig < 0) maxSeedsPerConfig = Integer.MAX_VALUE;
		this.instances = instances;
		this.maxSeedsPerConfig = maxSeedsPerConfig;
		seeds = new ArrayList<Queue<Long>>(instances.size());
		int i=0;
		
		this.instanceOrder = Collections.emptyList();
		for(String s : instances.keySet())
		{
			piKeyToIntMap.put(s, i++);
		}
		
		
		reinit();
	}
	*/
	public SetInstanceSeedGenerator(
			LinkedHashMap<String, List<Long>> instances, List<String> instanceOrder,  int maxSeedsPerConfig) {
		if(maxSeedsPerConfig < 0) maxSeedsPerConfig = Integer.MAX_VALUE;
		this.instances = instances;
		this.maxSeedsPerConfig = maxSeedsPerConfig;
		this.instanceOrder = instanceOrder;
		seeds = new ArrayList<Queue<Long>>(instances.size());
		int i=0;
		
		//Stores the total number of seeds
		int seedSum = 0;
		
		//Stores the total 
		int lastSeedCount = -1;
		
		boolean allSeedsSame = true;
		for(Entry<String, List<Long>> ent : instances.entrySet())
		{
			piKeyToIntMap.put(ent.getKey(), i++);
			
			seedSum+= ent.getValue().size();
			
			if(lastSeedCount != -1)
			{
				if(allSeedsSame)
				{
					allSeedsSame = lastSeedCount==ent.getValue().size();
				}
			} else
			{
				lastSeedCount = ent.getValue().size();
			}
		}
		
		this.initialSeedCount = seedSum;
		this.allInstanceHaveSameNumberOfSeeds = allSeedsSame;
		
		
		reinit();
	}
	
	
	
	

	@Override
	public void reinit() {
		seeds.clear();
		
		for(Entry<String, List<Long>> ent : instances.entrySet())
		{
			seeds.add(new LinkedList<Long>(ent.getValue()));
		}
		

	}

	@Override
	public int getNextSeed(ProblemInstance pi) {
		if(hasNextSeed(pi))
		{
			return getNextSeed(piKeyToIntMap.get(pi.getInstanceName()));
		} else
		{
			throw new IllegalStateException("No more Seeds for Problem Instance: " + pi.getInstanceName());
		}
		
	}

	
	@Override
	public int getNextSeed(Integer id) {
		if(id == null)
		{
			throw new IllegalArgumentException("Unrecognized Problem Instance " + id);
		} else
		{
			return (int) (long) seeds.get(id).poll();
		}
	}

	@Override
	public boolean hasNextSeed(ProblemInstance pi) {
		
		
		return !seeds.get(piKeyToIntMap.get(pi.getInstanceName())).isEmpty();
	}
	
	@Override
	public List<ProblemInstance> getProblemInstanceOrder(Collection<ProblemInstance> c)
	{
		Map<String, ProblemInstance> instanceMap = new HashMap<String, ProblemInstance>();
		
		for(ProblemInstance pi : c)
		{
			instanceMap.put(pi.getInstanceName(), pi);
		}
		
		
		List<ProblemInstance> piOrder = new ArrayList<ProblemInstance>(instanceOrder.size());
		for(String instance : instanceOrder)
		{
			ProblemInstance pi = instanceMap.get(instance);
			if(pi == null)
			{
				pi = instanceMap.get(instance.replaceAll("//", "/"));
			}
			
			if(pi == null)
			{
				throw new IllegalStateException("Couldn't find instance that matches : " + instance );
			}
			piOrder.add(pi);
		}
		
		return piOrder;
	}





	@Override
	public int getInitialSeedCount() {

		return initialSeedCount;
	}





	@Override
	public boolean allInstancesHaveSameNumberOfSeeds() {

		return allInstanceHaveSameNumberOfSeeds;
	}

}