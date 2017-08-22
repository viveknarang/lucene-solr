package org.apache.solr.tests.nightlybenchmarks;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BenchmarksMain {

	final static int NODES = 5;
	public static void main(String[] args) throws IOException, ParseException {
		// TODO Auto-generated method stub

		SolrCloud solrCloud = new SolrCloud(NODES); // zookeeper 
		StandaloneSolr standalone = new new StandaloneSolr(); // port
		
		String jsonConfig = FileUtils.readFileToString(new File("config.json"),"UTF-8");
		//JSONObject config = new JSONO
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(jsonConfig);
		
		
		// ---- INDEXING ----
		JSONArray indexBenchmarks = (JSONArray)json.get("index-benchmarks");
		//System.out.println(indexBenchmarks);
		
		Iterator it = indexBenchmarks.iterator();
		while (it.hasNext()) {
			JSONObject benchmark = (JSONObject)it.next();
			
			String name = (String)benchmark.get("name");
			String description = (String)benchmark.get("description");
			String datasetFile = (String)benchmark.get("dataset-file");
			String replicationType = (String)benchmark.get("replication-type");
			JSONArray setups = (JSONArray) benchmark.get("setups");
			Iterator setupsIterator = setups.iterator();
			while (setupsIterator.hasNext()) {
				JSONObject setup = (JSONObject) setupsIterator.next();
				String collectionName = (String) setup.get("collection");
				String replicationFactor = (String) setup.get("replicationFactor");
				String shards = (String) setup.get("shard");
				int minThreads = (int) setup.get("min-threads");
				int maxThreads = (int) setup.get("max-threads");

				File outputCSV = new File(collectionName+".csv");

				for (int i=minThreads; i<=maxThreads; i++) {
					if ("cloud".equals(replicationType)) {
						createCollection(solrCloud, collectionName, replicationFactor, shards)
						index (solrCloud, collectionName, i, datasetFile, outputCSV);
						if (i != maxThreads) {
							deleteCollection(solrCloud, collectionName);
						}
					} else {
						createCore(standalone, coreName);
						index (standalone, collectionName, i, datasetFile, outputCSV);
						if (i != maxThreads) {
							deleteCore(standalone, collectionName);
						}
						
					}
					
				}
			}
			
		}
		
		
		// do the same for query benchmarks
		// ---- QUERYING ----
		solrCloud.close()
		standalone.close();
	}

	static void index(SolrCloud solrCloud, String collectionName, int threads, String datasetFile, File outputCSV) {
		
		if (datasetFile.contains("wiki")) {
			// logic to index wikipedia into collectionName
			
			// start the indexing using given threads
		}
	}

	static void index(StandaloneSolr standalone, String collectionName, int threads, String datasetFile, File outputCSV) {
		HttpSolrClient hsc = new HttpSolrClient(standalone.getUrl());
		
	}

}
