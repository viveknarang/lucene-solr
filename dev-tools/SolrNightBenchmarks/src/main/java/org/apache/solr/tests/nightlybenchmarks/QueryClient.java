package org.apache.solr.tests.nightlybenchmarks;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

public class QueryClient implements Runnable {

	public enum QueryType {
		TERM_NUMERIC_QUERY, RANGE_NUMERIC_QUERY, GREATER_THAN_NUMERIC_QUERY, LESS_THAN_NUMERIC_QUERY
	}

	String urlString;
	int queueSize;
	int threadCount;
	String collectionName;
	SolrParams params;
	ConcurrentUpdateSolrClient solrClient;
	QueryType queryType;
	int threadID;
	boolean setThreadReadyFlag = false;
	long numberOfThreads = 0;
	long startTime = 0;
	long delayEstimationBySeconds = 0;

	public static boolean running;
	public static long queryCount = 0;
	public static long minQtime = Long.MAX_VALUE;
	public static long maxQtime = Long.MIN_VALUE;
	public static long queryFailureCount = 0;
	public static long threadReadyCount = 0;
	public static DescriptiveStatistics percentiles;
	public static boolean percentilesObjectCreated = false;
	public static long[] qTimePercentileList = new long[10000000];
	public static int qTimePercentileListPointer = 0;

	Random random = new Random();

	@SuppressWarnings("deprecation")
	public QueryClient(String urlString, int queueSize, int threadCount, String collectionName, QueryType queryType,
			long numberOfThreads, long delayEstimationBySeconds) {
		super();
		this.urlString = urlString;
		this.queueSize = queueSize;
		this.threadCount = threadCount;
		this.collectionName = collectionName;
		this.queryType = queryType;
		this.numberOfThreads = numberOfThreads;
		this.delayEstimationBySeconds = delayEstimationBySeconds;

		solrClient = new ConcurrentUpdateSolrClient(urlString, queueSize, threadCount);
		Util.postMessage("\r" + this.toString() + "** QUERY CLIENT CREATED ...", MessageType.RED_TEXT, false);
	}

	public void run() {

		long elapsedTime;

		startTime = System.nanoTime();
		while (true) {

			if (!setThreadReadyFlag) {
				setThreadReadyFlag = true;
				setThreadReadyCount();
			}

			if (running == true) {
				// Critical Section ....
				SolrResponse response = null;
				try {
					NamedList<String> list = new NamedList<>();
					list.add("defType", "edismax");
					list.add("wt", "json");

					if (this.queryType == QueryType.TERM_NUMERIC_QUERY) {
						list.add("q", "RandomIntField:"
								+ SolrIndexingClient.intList.get(random.nextInt(SolrIndexingClient.documentCount)));
					} else if (this.queryType == QueryType.RANGE_NUMERIC_QUERY) {

						int ft_1 = SolrIndexingClient.intList.get(random.nextInt(SolrIndexingClient.documentCount));
						int ft_2 = SolrIndexingClient.intList.get(random.nextInt(SolrIndexingClient.documentCount));

						if (ft_2 > ft_1) {
							list.add("q", "RandomIntField:[" + ft_1 + " TO " + ft_2 + "]");
						} else {
							list.add("q", "RandomIntField:[" + ft_2 + " TO " + ft_1 + "]");
						}

					} else if (this.queryType == QueryType.GREATER_THAN_NUMERIC_QUERY) {
						list.add("q", "RandomIntField:["
								+ SolrIndexingClient.intList.get(random.nextInt(SolrIndexingClient.documentCount))
								+ " TO *]");
					} else if (this.queryType == QueryType.LESS_THAN_NUMERIC_QUERY) {
						list.add("q", "RandomIntField:[* TO "
								+ SolrIndexingClient.intList.get(random.nextInt(SolrIndexingClient.documentCount))
								+ "]");
					}

					params = SolrParams.toSolrParams(list);

					response = this.fireQuery(collectionName, params);

					if ((System.nanoTime() - startTime) >= (delayEstimationBySeconds * 1000000000)) {
						setQueryCounter();
						elapsedTime = response.getElapsedTime();
						setMinMaxQTime(elapsedTime);
					}

				} catch (SolrServerException | IOException e) {
					setQueryFailureCount();
				}

			} else if (running == false) {
				// Break out from loop ...
				Util.postMessage("\r" + this.toString() + "** Getting out of critical section ...",
						MessageType.RED_TEXT, false);
				break;
			}
		}

		solrClient.close();
		return;
	}

	private synchronized SolrResponse fireQuery(String collectionName, SolrParams params)
			throws SolrServerException, IOException {

		return solrClient.query(collectionName, params);

	}

	private synchronized void setQueryCounter() {

		if (running == false) {
			return;
		}

		queryCount++;
	}

	private synchronized void setQueryFailureCount() {

		queryFailureCount++;

	}

	private synchronized void setThreadReadyCount() {

		threadReadyCount++;

	}

	private synchronized void setMinMaxQTime(long QTime) {

		if (running == false) {
			return;
		}
		
		qTimePercentileList[qTimePercentileListPointer++] = QTime;
		
		if (QTime < minQtime) {
			minQtime = QTime;
		}

		if (QTime > maxQtime) {
			maxQtime = QTime;
		}

	}

	public static double getNthPercentileQTime(double percentile) {

		if(!percentilesObjectCreated) {
	
			double[] finalQtime = new double[qTimePercentileListPointer];
			System.out.println(qTimePercentileListPointer);
			for (int i = 0; i < (finalQtime.length); i++) {
					finalQtime[i] = qTimePercentileList[i];
			}			
			Arrays.sort(finalQtime);			
			percentiles = new DescriptiveStatistics(finalQtime);
			percentilesObjectCreated = true;
		}
		
		return percentiles.getPercentile(percentile);

	}

	public static void reset() {
		running = false;
		queryCount = 0;
		minQtime = Long.MAX_VALUE;
		maxQtime = Long.MIN_VALUE;
		queryFailureCount = 0;
		threadReadyCount = 0;
		percentiles = null;
		percentilesObjectCreated = false;
	}

}
