package org.apache.solr.tests.nightlybenchmarks;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.solr.tests.nightlybenchmarks.BenchmarkAppConnector.FileType;

public class Benchmarks {

	public static List<BenchmarkConfiguration> configurations;
	public static List<BenchmarkResponse> responses = new LinkedList<BenchmarkResponse>();

	public final static Logger logger = Logger.getLogger(Benchmarks.class);

	private static Map<String, String> numericQueryTests(String commitID, int numberOfThreads, String baseURL,
			String collectionName, String queryClientType, String zookeeperURL, long numberOfQueriesToRun,
			String queryFileName) throws Exception {
		
		System.out.println("####################### " + queryFileName);

		int delayEstimationBySeconds = 10;

		try {
			QueryClient.reset();
			QueryClient.queryFileName = queryFileName;
			QueryClient.prepare();
			QueryClient.queryCountLimit = numberOfQueriesToRun;

			CountDownLatch latch = new CountDownLatch(numberOfThreads);
			ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
			LinkedList<QueryClient> list = new LinkedList<QueryClient>();

			for (int i = 0; i < numberOfThreads; i++) {
				QueryClient client = new QueryClient(baseURL, collectionName, numberOfThreads, delayEstimationBySeconds,
						queryClientType, zookeeperURL, latch);
				list.add(client);
			}

			QueryClient.running = true;

			for (int i = 0; i < numberOfThreads; i++) {
				executorService.execute(list.get(i));
			}

			latch.await();

			executorService.shutdownNow();

			Thread.sleep(5000);

			Map<String, String> returnMap = new HashMap<String, String>();

			Date dNow = new Date();
			SimpleDateFormat ft = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

			returnMap.put("TimeStamp", ft.format(dNow));
			returnMap.put("CommitID", commitID);
			returnMap.put("TotalQueriesExecuted", "" + QueryClient.queryCount);
			returnMap.put("QueriesPerSecond", "" + ((float) QueryClient.queryCount
					/ (float) ((QueryClient.endTime - QueryClient.startTime) / 1000d)));
			returnMap.put("MinQTime", "" + QueryClient.minQtime);
			returnMap.put("MaxQTime", "" + QueryClient.maxQtime);
			returnMap.put("QueryFailureCount", "" + QueryClient.queryFailureCount);
			returnMap.put("TotalQTime", "" + QueryClient.totalQTime);
			returnMap.put("75thQtime", "" + QueryClient.getNthPercentileQTime(75));
			returnMap.put("95thQtime", "" + QueryClient.getNthPercentileQTime(95));
			returnMap.put("99thQtime", "" + QueryClient.getNthPercentileQTime(99));
			returnMap.put("99.9thQtime", "" + QueryClient.getNthPercentileQTime(99.9));
			returnMap.put("startTime", "" + QueryClient.startTime);
			returnMap.put("endTime", "" + QueryClient.endTime);
			returnMap.put("activeTime(MS)", "" + (QueryClient.endTime - QueryClient.startTime));

			logger.debug(returnMap.toString());
			QueryClient.reset();

			return returnMap;

		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new Exception(e.getMessage());
		}
	}

	public static void runBenchmarks(String commitID) {

		configurations = Util.getBenchmarkConfigurations();
		System.out.println(configurations.toString());

		try {

			for (BenchmarkConfiguration configuration : configurations) {

				configuration.commitID = commitID;

				if (configuration.benchmarkType.equals("Indexing")) {

					if (configuration.benchmarkOn.equals("SolrStandaloneMode")) {

						SolrNode node = new SolrNode(configuration.commitID, "", "", false);
						node.doAction(SolrNodeAction.NODE_START);
						node.createCollection(configuration, "Core-" + UUID.randomUUID(),
								"Collection-" + UUID.randomUUID());

						Thread.sleep(5000);

						SolrIndexingClient client = new SolrIndexingClient("localhost", node.port,
								configuration.commitID);

						responses.add(new BenchmarkResponse(client.indexData(configuration,
								node.getBaseUrl() + node.collectionName, null, 0, true, true, null, null)));

						node.doAction(SolrNodeAction.NODE_STOP);
						node.cleanup();

					} else if (configuration.benchmarkOn.equals("SolrCloudMode")) {

						SolrCloud cloud = new SolrCloud(configuration, null, true, "localhost");
						SolrIndexingClient client = new SolrIndexingClient("localhost", cloud.port,
								configuration.commitID);

						responses.add(new BenchmarkResponse(client.indexData(configuration, cloud.getuRL(),
								cloud.collectionName, 100, true, true, cloud.zookeeperIp, cloud.zookeeperPort)));

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
						node.createCollection(configuration, "Core-" + UUID.randomUUID(),
								"Collection-" + UUID.randomUUID());

						Thread.sleep(5000);

						SolrIndexingClient client = new SolrIndexingClient("localhost", node.port,
								configuration.commitID);
						Map<String, String> output = client.indexData(configuration,
								node.getBaseUrl() + node.collectionName, null, 0, true, true, null, null);

						BenchmarkAppConnector.writeToWebAppDataFile(i.outputFile, output.toString(), false,
								FileType.GENERIC);

						responses.add(new BenchmarkResponse(output));

						node.doAction(SolrNodeAction.NODE_STOP);
						node.cleanup();

					}
				}
			}

			List<QueryBenchmark> querying = configurationM.queryBenchmarks;

			for (QueryBenchmark i : querying) {
				
				System.out.println(i.toString());

				BenchmarkConfiguration configuration = new BenchmarkConfiguration();

				configuration.benchmarkClient = i.clientType;
				configuration.benchmarkOn = i.replicationType;
				configuration.inputCount = Integer.parseInt(i.inputCount);
				configuration.commitID = commitID;
				configuration.benchmarkType = i.benchmarkType;
				configuration.benchmarkSubType = i.benchmarkSubType;
				configuration.fileName = i.dataSetFile;

				System.out.println(configuration.toString());

				if (configuration.benchmarkType.equals("Querying")) {

					if (configuration.benchmarkOn.equals("SolrStandaloneMode")) {

						SolrNode node = new SolrNode(configuration.commitID, "", "", false);
						node.doAction(SolrNodeAction.NODE_START);
						node.createCollection(configuration, "Core-" + UUID.randomUUID(),
								"Collection-" + UUID.randomUUID());

						Thread.sleep(5000);

						SolrIndexingClient client = new SolrIndexingClient("localhost", node.port,
								configuration.commitID);
						client.indexData(configuration, node.getBaseUrl() + node.collectionName, null, 0, true, false,
								null, null);

						Map<String, String> output = Benchmarks.numericQueryTests(configuration.commitID, i.minThreads, node.getBaseUrl(),
								node.collectionName, configuration.benchmarkClient, "", i.numberOfQueries,
								i.queryFile);

						BenchmarkAppConnector.writeToWebAppDataFile(i.outputFile, output.toString(), false,
								FileType.GENERIC);
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