/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.planner.benchmark.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.comparators.ReverseComparator;
import org.drools.planner.benchmark.api.ranking.SolverBenchmarkRankingWeightFactory;
import org.drools.planner.benchmark.api.PlannerBenchmark;
import org.drools.planner.benchmark.core.statistic.PlannerStatistic;
import org.drools.planner.config.SolverFactory;
import org.drools.planner.core.Solver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the benchmarks on multiple {@link Solver} configurations on multiple problem instances (data sets).
 */
public class DefaultPlannerBenchmark implements PlannerBenchmark {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private File benchmarkDirectory = null;
    private File benchmarkReportDirectory = null;
    private Comparator<SolverBenchmark> solverBenchmarkRankingComparator = null;
    private SolverBenchmarkRankingWeightFactory solverBenchmarkRankingWeightFactory = null;

    private int parallelBenchmarkCount = -1;
    private long warmUpTimeMillisSpend = 0L;

    private List<SolverBenchmark> solverBenchmarkList = null;
    private List<ProblemBenchmark> unifiedProblemBenchmarkList = null;

    private Date startingTimestamp;
    private String plannerVersion;
    private ExecutorService executorService;
    private Integer failureCount;
    private Throwable firstFailureThrowable;
    private SolverBenchmark winningSolverBenchmark;

    public File getBenchmarkDirectory() {
        return benchmarkDirectory;
    }

    public void setBenchmarkDirectory(File benchmarkDirectory) {
        this.benchmarkDirectory = benchmarkDirectory;
    }

    public File getBenchmarkReportDirectory() {
        return benchmarkReportDirectory;
    }

    public Comparator<SolverBenchmark> getSolverBenchmarkRankingComparator() {
        return solverBenchmarkRankingComparator;
    }

    public void setSolverBenchmarkRankingComparator(Comparator<SolverBenchmark> solverBenchmarkRankingComparator) {
        this.solverBenchmarkRankingComparator = solverBenchmarkRankingComparator;
    }

    public SolverBenchmarkRankingWeightFactory getSolverBenchmarkRankingWeightFactory() {
        return solverBenchmarkRankingWeightFactory;
    }

    public void setSolverBenchmarkRankingWeightFactory(SolverBenchmarkRankingWeightFactory solverBenchmarkRankingWeightFactory) {
        this.solverBenchmarkRankingWeightFactory = solverBenchmarkRankingWeightFactory;
    }

    public int getParallelBenchmarkCount() {
        return parallelBenchmarkCount;
    }

    public void setParallelBenchmarkCount(int parallelBenchmarkCount) {
        this.parallelBenchmarkCount = parallelBenchmarkCount;
    }

    public long getWarmUpTimeMillisSpend() {
        return warmUpTimeMillisSpend;
    }

    public void setWarmUpTimeMillisSpend(long warmUpTimeMillisSpend) {
        this.warmUpTimeMillisSpend = warmUpTimeMillisSpend;
    }

    public List<SolverBenchmark> getSolverBenchmarkList() {
        return solverBenchmarkList;
    }

    public void setSolverBenchmarkList(List<SolverBenchmark> solverBenchmarkList) {
        this.solverBenchmarkList = solverBenchmarkList;
    }

    public List<ProblemBenchmark> getUnifiedProblemBenchmarkList() {
        return unifiedProblemBenchmarkList;
    }

    public void setUnifiedProblemBenchmarkList(List<ProblemBenchmark> unifiedProblemBenchmarkList) {
        this.unifiedProblemBenchmarkList = unifiedProblemBenchmarkList;
    }

    public Date getStartingTimestamp() {
        return startingTimestamp;
    }

    /**
     * @return sometimes null (only during development)
     */
    public String getPlannerVersion() {
        return plannerVersion;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    // ************************************************************************
    // Benchmark methods
    // ************************************************************************

    public void benchmark() {
        benchmarkingStarted();
        warmUp();
        runSingleBenchmarks();
        benchmarkingEnded();
    }

    public void benchmarkingStarted() {
        startingTimestamp = new Date();
        if (solverBenchmarkList == null || solverBenchmarkList.isEmpty()) {
            throw new IllegalArgumentException(
                    "The solverBenchmarkList (" + solverBenchmarkList + ") cannot be empty.");
        }
        plannerVersion = SolverFactory.class.getPackage().getImplementationVersion();
        initBenchmarkDirectoryAndSubdirs();
        for (SolverBenchmark solverBenchmark : solverBenchmarkList) {
            solverBenchmark.benchmarkingStarted();
        }
        for (ProblemBenchmark problemBenchmark : unifiedProblemBenchmarkList) {
            problemBenchmark.benchmarkingStarted();
        }
        executorService = Executors.newFixedThreadPool(parallelBenchmarkCount);
        failureCount = 0;
        firstFailureThrowable = null;
        winningSolverBenchmark = null;
        logger.info("Benchmarking started: solverBenchmarkList size ({}), parallelBenchmarkCount ({}).",
                solverBenchmarkList.size(), parallelBenchmarkCount);
    }

    private void initBenchmarkDirectoryAndSubdirs() {
        if (benchmarkDirectory == null) {
            throw new IllegalArgumentException("The benchmarkDirectory (" + benchmarkDirectory + ") must not be null.");
        }
        benchmarkDirectory.mkdirs();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(startingTimestamp);
        benchmarkReportDirectory = new File(benchmarkDirectory, timestamp);
        benchmarkReportDirectory.mkdirs();
    }

    private void warmUp() {
        if (warmUpTimeMillisSpend > 0L) {
            logger.info("================================================================================");
            logger.info("Warming up");
            logger.info("================================================================================");
            long startingTimeMillis = System.currentTimeMillis();
            long timeLeft = warmUpTimeMillisSpend;
            Iterator<ProblemBenchmark> it = unifiedProblemBenchmarkList.iterator();
            while (timeLeft > 0L) {
                if (!it.hasNext()) {
                    it = unifiedProblemBenchmarkList.iterator();
                }
                ProblemBenchmark problemBenchmark = it.next();
                timeLeft = problemBenchmark.warmUp(startingTimeMillis, warmUpTimeMillisSpend, timeLeft);
            }
            logger.info("================================================================================");
            logger.info("Finished warmUp");
            logger.info("================================================================================");
        }
    }

    protected void runSingleBenchmarks() {
        Map<SingleBenchmark, Future<SingleBenchmark>> futureMap
                = new HashMap<SingleBenchmark, Future<SingleBenchmark>>();
        for (ProblemBenchmark problemBenchmark : unifiedProblemBenchmarkList) {
            for (SingleBenchmark singleBenchmark : problemBenchmark.getSingleBenchmarkList()) {
                Future<SingleBenchmark> future = executorService.submit(singleBenchmark);
                futureMap.put(singleBenchmark, future);
            }
        }
        // wait for the benchmarks to complete
        for (Map.Entry<SingleBenchmark, Future<SingleBenchmark>> futureEntry : futureMap.entrySet()) {
            SingleBenchmark singleBenchmark = futureEntry.getKey();
            Future<SingleBenchmark> future = futureEntry.getValue();
            Throwable failureThrowable = null;
            try {
                // Explicitly returning it in the Callable guarantees memory visibility
                singleBenchmark = future.get();
                // TODO WORKAROUND Remove when JBRULES-3462 is fixed.
                if (singleBenchmark.getScore() == null) {
                    throw new IllegalStateException("Score is null. TODO fix JBRULES-3462.");
                }
            } catch (InterruptedException e) {
                logger.error("The singleBenchmark (" + singleBenchmark.getName() + ") was interrupted.", e);
                failureThrowable = e;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                logger.error("The singleBenchmark (" + singleBenchmark.getName() + ") failed.", cause);
                failureThrowable = cause;
            } catch (IllegalStateException e) {
                // TODO WORKAROUND Remove when JBRULES-3462 is fixed.
                logger.error("The singleBenchmark (" + singleBenchmark.getName() + ") failed.", e);
                failureThrowable = e;
            }
            if (failureThrowable == null) {
                singleBenchmark.setSucceeded(true);
            } else {
                singleBenchmark.setSucceeded(false);
                singleBenchmark.setFailureThrowable(failureThrowable);
                failureCount++;
                if (firstFailureThrowable == null) {
                    firstFailureThrowable = failureThrowable;
                }
            }
        }
    }

    public void benchmarkingEnded() {
        executorService.shutdownNow();
        for (ProblemBenchmark problemBenchmark : unifiedProblemBenchmarkList) {
            problemBenchmark.benchmarkingEnded();
        }
        for (SolverBenchmark solverBenchmark : solverBenchmarkList) {
            solverBenchmark.benchmarkingEnded();
        }
        determineRanking();
        PlannerStatistic plannerStatistic = new PlannerStatistic(this);
        plannerStatistic.writeStatistics();
        if (failureCount == 0) {
            logger.info("Benchmarking ended: winning solverBenchmark ({}), statistic html overview ({}).",
                    winningSolverBenchmark.getName(), plannerStatistic.getHtmlOverviewFile().getAbsolutePath());
        } else {
            logger.info("Benchmarking failed: failureCount ({}), statistic html overview ({}).",
                    failureCount, plannerStatistic.getHtmlOverviewFile().getAbsolutePath());
            throw new IllegalStateException("Benchmarking failed: failureCount (" + failureCount + ")." +
                    " The first exception is chained.",
                    firstFailureThrowable);
        }
    }

    private void determineRanking() {
        List<SolverBenchmark> rankedSolverBenchmarkList = new ArrayList<SolverBenchmark>(solverBenchmarkList);
        // Do not rank a SolverBenchmark that has a failure
        for (Iterator<SolverBenchmark> it = rankedSolverBenchmarkList.iterator(); it.hasNext(); ) {
            SolverBenchmark solverBenchmark = it.next();
            if (solverBenchmark.hasAnyFailure()) {
                it.remove();
            }
        }
        if (solverBenchmarkRankingComparator != null) {
            Collections.sort(rankedSolverBenchmarkList, Collections.reverseOrder(solverBenchmarkRankingComparator));
        } else if (solverBenchmarkRankingWeightFactory != null) {
            SortedMap<Comparable, SolverBenchmark> rankedSolverBenchmarkMap = new TreeMap<Comparable, SolverBenchmark>(
                    new ReverseComparator());
            for (SolverBenchmark solverBenchmark : rankedSolverBenchmarkList) {
                Comparable rankingWeight = solverBenchmarkRankingWeightFactory.createRankingWeight(
                        rankedSolverBenchmarkList, solverBenchmark);
                Object previous = rankedSolverBenchmarkMap.put(rankingWeight, solverBenchmark);
                if (previous != null) {
                    throw new IllegalStateException("The solverBenchmarkList contains 2 times"
                            + " the same solverBenchmark (" + previous + ") and (" + solverBenchmark + ").");
                }
            }
            rankedSolverBenchmarkList.clear();
            rankedSolverBenchmarkList.addAll(rankedSolverBenchmarkMap.values());
        } else {
            throw new IllegalStateException("Ranking is impossible" +
                    " because solverBenchmarkRankingComparator and solverBenchmarkRankingWeightFactory are null.");
        }
        int ranking = 0;
        for (SolverBenchmark solverBenchmark : rankedSolverBenchmarkList) {
            solverBenchmark.setRanking(ranking);
            ranking++;
        }
        winningSolverBenchmark = rankedSolverBenchmarkList.isEmpty() ? null : rankedSolverBenchmarkList.get(0);
    }

    public boolean hasAnyFailure() {
        return failureCount > 0;
    }

    // TODO Temporarily disabled because it crashes because of http://jira.codehaus.org/browse/XSTR-666
//    public void writeBenchmarkResult(XStream xStream) {
//        File benchmarkResultFile = new File(benchmarkReportDirectory, "benchmarkResult.xml");
//        OutputStreamWriter writer = null;
//        try {
//            writer = new OutputStreamWriter(new FileOutputStream(benchmarkResultFile), "UTF-8");
//            xStream.toXML(this, writer);
//        } catch (UnsupportedEncodingException e) {
//            throw new IllegalStateException("This JVM does not support UTF-8 encoding.", e);
//        } catch (FileNotFoundException e) {
//            throw new IllegalArgumentException(
//                    "Could not create benchmarkResultFile (" + benchmarkResultFile + ").", e);
//        } finally {
//            IOUtils.closeQuietly(writer);
//        }
//    }

}