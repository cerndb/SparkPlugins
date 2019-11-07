/*
 * Copyright 2019 CERN IT
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

package com.oracle.bmc.hdfs.store;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/* This class implements instrumentation for BMCFSInputStream and BMCOutputStream
 * In particular it implements time measurement, introduced for performance troubleshooting
 * of Spark workloads
 * BytesRead are also implemented for completeness, although already available via
 * standard Hadoop Statistics instrumentation.
 * ReadCalls is also implemented, although of limited utility as it is not the the number of read
 * operations,the difference comes from prefetching/readahead.
 * For HadoopFS statistics, see: org.apache.hadoop.fs.FileSystem.printStatistics()
 */

public class BmcTimeInstrumentation {
    private static AtomicLong timeElapsedReadMusec = new AtomicLong();
    private static AtomicLong timeElapsedSeekTime = new AtomicLong();
    private static AtomicLong timeCPUDuringReadMusec = new AtomicLong();
    private static AtomicLong timeCPUDuringSeekMusec = new AtomicLong();
    private static AtomicInteger readCalls = new AtomicInteger();
    private static AtomicInteger seekCalls = new AtomicInteger();
    private static AtomicLong bytesRead = new AtomicLong();

    /* Time spent for read calls in BMCInputStream */
    public static long getTimeElapsedReadMusec() {
        return timeElapsedReadMusec.get();
    }

    /* CPU time spent during read calls in BMCInputStream */
    public static long getCPUTimeDuringReadMusec() {
        return timeCPUDuringReadMusec.get();
    }

    /* Increment time spent during read calls in BMCInputStream */
    public static void incrementCPUTimeDuringRead(Long incrementTime) {
        timeCPUDuringReadMusec.getAndAdd(incrementTime);
    }

    /* CPU time spent during seek calls in BMCInputStream */
    public static long getCPUTimeDuringSeekMusec() {
        return timeCPUDuringSeekMusec.get();
    }

    /* Increment time spent during seek calls in BMCInputStream */
    public static void incrementCPUTimeDuringSeek(Long incrementTime) {
        timeCPUDuringSeekMusec.getAndAdd(incrementTime);
    }

    public static void incrementTimeElapsedReadOps(Long incrementTime) {
        timeElapsedReadMusec.getAndAdd(incrementTime);
    }

    /* Time spent for seek calls in BMCInputStream */
    public static long getTimeElapsedSeekMusec() {
        return timeElapsedSeekTime.get();
    }

    public static void incrementTimeElapsedSeekOps(Long incrementTime) {
        timeElapsedSeekTime.getAndAdd(incrementTime);
    }

    /* number of read calls in BMCInputStream, due to buffering/readahead this is not the number of
    * read operations, for that metrics see standard Hadoop statics
    */
    public static int getReadCalls() {
        return readCalls.get();
    }

    public static void incrementReadCalls(int numOps) {
        readCalls.getAndAdd(numOps);
    }

    /* number of seek operation calls in BMCInputStream */
    public static int getSeekCalls() {
        return seekCalls.get();
    }

    public static void incrementSeekCalls(int numOps) {
        seekCalls.getAndAdd(numOps);
    }

    /* bytes read in BMCInputStream */
    public static long getBytesRead() {
        return bytesRead.get();
    }

    public static void incrementBytesRead(Long incrementBytesRead) {
        bytesRead.getAndAdd(incrementBytesRead);
    }

    /* TODO: implement metrics for output stream */

}
