package org.apache.solr.tests.nightlybenchmarks;

import java.util.List;

public class BenchmarkNConfiguration {

    public List<IndexBenchmark> indexBenchmarks;
    public List<QueryBenchmark> queryBenchmarks;
	
    @Override
	public String toString() {
		return "BenchmarkNConfiguration [indexBenchmarks=" + indexBenchmarks + ", queryBenchmarks=" + queryBenchmarks
				+ "]";
	}

}

class IndexBenchmark {
	
	public String name;
	public String description;
	public String replicationType;
	public String dataSetFile;
	public String clientType;
	public String inputCount;
	public String benchmarkType;
	public String benchmarkSubType;
	public String outputFile;
	public List<Setup> setups;
	
	@Override
	public String toString() {
		return "IndexBenchmark [name=" + name + ", description=" + description + ", replicationType=" + replicationType
				+ ", dataSetFile=" + dataSetFile + ", clientType=" + clientType + ", inputCount=" + inputCount
				+ ", benchmarkType=" + benchmarkType + ", benchmarkSubType=" + benchmarkSubType + ", outputFile="
				+ outputFile + ", setups=" + setups + "]";
	}
}

class Setup {
	
	public String collection;
	public int replicationFactor;
	public int shards;
	public int minThreads;
	public int maxThreads;
	
	@Override
	public String toString() {
		return "Setup [collection=" + collection + ", replicationFactor=" + replicationFactor + ", shards=" + shards
				+ ", minThreads=" + minThreads + ", maxThreads=" + maxThreads + "]";
	}	
}

class QueryBenchmark {
	
	public String name;
	public String description;
	public String replicationType;
	public String collectionCore;
	public String queryFile;
	public String clientType;
	public int minThreads;
	public int maxThreads;
	public String benchmarkType;
	public String benchmarkSubType;
	public String inputCount;
	public String outputFile;
	public String dataSetFile;
	public long numberOfQueries;
	
	@Override
	public String toString() {
		return "QueryBenchmark [name=" + name + ", description=" + description + ", replicationType=" + replicationType
				+ ", collectionCore=" + collectionCore + ", queryFile=" + queryFile + ", clientType=" + clientType
				+ ", minThreads=" + minThreads + ", maxThreads=" + maxThreads + ", benchmarkType=" + benchmarkType
				+ ", benchmarkSubType=" + benchmarkSubType + ", inputCount=" + inputCount + ", outputFile=" + outputFile
				+ ", dataSetFile=" + dataSetFile + "]";
	}
}