package org.apache.solr.tests.nightlybenchmarks;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.solr.tests.nightlybenchmarks.BenchmarkAppConnector.FileType;

public class Benchmarks {
	
	public static List<BenchmarkConfiguration> configurations;
	public static List<BenchmarkResponse> responses = new LinkedList<BenchmarkResponse>();
	
	public static void runBenchmarks(String commitID) {
		
		configurations = Util.getBenchmarkConfigurations();
		System.out.println(configurations.toString());
		
		try {
	
			for(BenchmarkConfiguration configuration : configurations) {
				
				configuration.commitID = commitID;
				
				if (configuration.benchmarkType.equals("Indexing")) {
					
					if (configuration.benchmarkOn.equals("SolrStandaloneMode")) {

						SolrNode node = new SolrNode(configuration.commitID, "", "", false);
						node.doAction(SolrNodeAction.NODE_START);
						node.createCollection(configuration, "Core-" + UUID.randomUUID(), "Collection-" + UUID.randomUUID());
						
						Thread.sleep(5000);
						
						SolrIndexingClient client = new SolrIndexingClient("localhost", node.port, configuration.commitID);

							responses.add(new BenchmarkResponse(client.indexData(configuration,
									node.getBaseUrl() + node.collectionName, null, 0, true, true, null, null)));
						
						node.doAction(SolrNodeAction.NODE_STOP);
						node.cleanup();
					
					} else if (configuration.benchmarkOn.equals("SolrCloudMode")) {
						
						SolrCloud cloud = new SolrCloud(configuration, null, true, "localhost");
						SolrIndexingClient client = new SolrIndexingClient("localhost", cloud.port, configuration.commitID);

							responses.add(new BenchmarkResponse(client.indexData(configuration,
									cloud.getuRL(),	cloud.collectionName, 100, true, true, cloud.zookeeperIp, cloud.zookeeperPort)));
						
						cloud.shutdown();
						
					}
				} else if (configuration.benchmarkType.equals("Querying")) {
					if (configuration.benchmarkOn.equals("SolrStandaloneMode")) {
						
						
						
					} else if (configuration.benchmarkOn.equals("SolrCloudMode")) {
						
						
					}
				}

				}
		} catch (Exception e) {
		}
	}
	
	public static void runIBenchmarks(String commitID) {
		
		
		BenchmarkNConfiguration configurationM = Util.getIBenchmarkConfigurations();
		
		System.out.println(configurationM.toString());		

		try {
				List<IndexBenchmark> indexing = configurationM.indexBenchmarks;
			
				for (IndexBenchmark i : indexing) {
				
					BenchmarkConfiguration configuration = new BenchmarkConfiguration();
					
					configuration.benchmarkClient = i.clientType;
					configuration.benchmarkOn = i.replicationType;
					configuration.inputCount = Integer.parseInt(i.inputCount);
					configuration.commitID = commitID;
					configuration.benchmarkType = i.benchmarkType;
					configuration.benchmarkSubType = i.benchmarkSubType;
					configuration.fileName = i.dataSetFile;
					
					System.out.println(configuration.toString());
					
					if (configuration.benchmarkType.equals("Indexing")) {
						
						if (configuration.benchmarkOn.equals("SolrStandaloneMode")) {
	
							SolrNode node = new SolrNode(configuration.commitID, "", "", false);
							node.doAction(SolrNodeAction.NODE_START);
							node.createCollection(configuration, "Core-" + UUID.randomUUID(), "Collection-" + UUID.randomUUID());
							
							Thread.sleep(5000);
							
							SolrIndexingClient client = new SolrIndexingClient("localhost", node.port, configuration.commitID);
							Map<String, String> output = client.indexData(configuration, node.getBaseUrl() + node.collectionName, null, 0, true, true, null, null);

							BenchmarkAppConnector.writeToWebAppDataFile(i.outputFile, output.toString(), false, FileType.GENERIC);
							
							responses.add(new BenchmarkResponse(output));
							
							node.doAction(SolrNodeAction.NODE_STOP);
							node.cleanup();
						
						} 
					}
				}
		} catch (Exception e) {
		}
	}
}