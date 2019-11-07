/*
 * Copyright 2014-2022 CERN IT
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
package org.apache.hadoop.fs.s3a;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class S3ATimeInstrumentation {

    private static AtomicLong timeElapsedReadMusec = new AtomicLong();
    private static AtomicLong timeElapsedSeekMusec = new AtomicLong();
    private static AtomicLong timeElapsedWriteMusec = new AtomicLong();
    private static AtomicLong timeCPUDuringReadMusec = new AtomicLong();
    private static AtomicLong timeCPUDuringSeekMusec = new AtomicLong();
    private static AtomicInteger readCalls = new AtomicInteger();
    private static AtomicInteger seekCalls = new AtomicInteger();
    private static AtomicInteger writeCalls = new AtomicInteger();
    private static AtomicLong bytesRead = new AtomicLong();
    private static AtomicLong bytesWritten = new AtomicLong();

    /**
     * Get the cumulative value of the elapsed time spent  by
     * the Hadoop Filesystem client waiting for S3A to return data of read
     * operations. The time is in microseconds.
     * Use from Hadoop clients, such as Spark environments, by calling
     * ch.cern.eos.S3AInstrumentation.getTimeElapsedReadMusec()
     *
     * @return cumulative elapsed time spend in read operations, in microseconds.
     */
    public static long getTimeElapsedReadMusec() {
        return timeElapsedReadMusec.get();
    }

    /**
     * Increment the value of the cumulative elapsed time spent  by
     * Hadoop Filesystem clients waiting for S3A to return data of read
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public static void incrementTimeElapsedReadOps(Long incrementTime) {
        timeElapsedReadMusec.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative elapsed time spent  by
     * Hadoop Filesystem clients waiting for S3A to return data of seek
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public static void incrementTimeElapsedSeekOps(Long incrementTime) {
        timeElapsedSeekMusec.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative CPU time spent by
     * Hadoop Filesystem clients while waiting for S3A to return data of read
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public static void incrementCPUTimeDuringRead(Long incrementTime) {
        timeCPUDuringReadMusec.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative CPU time spent by
     * Hadoop Filesystem clients while waiting for S3A to return data of seek
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public static void incrementCPUTimeDuringSeek(Long incrementTime) {
        timeCPUDuringSeekMusec.getAndAdd(incrementTime);
    }

    /**
     * Get the cumulative value of the elapsed time spent  by
     * the Hadoop Filesystem client waiting for S3A to perform seek
     * operations. The time is in microseconds.
     *
     * @return cumulative elapsed time spend in write operations, in microseconds.
     */

    public static long getTimeElapsedSeekMusec() {
        return timeElapsedSeekMusec.get();
    }

    /**
     * Get the cumulative value of the CPU time spent  by
     * the Hadoop Filesystem client while waiting for S3A to perform read
     * operations. The time is in microseconds.
     *
     * @return cumulative elapsed time spend in write operations, in microseconds.
     */
    public static long getCPUTimeDuringReadMusec() {
        return timeCPUDuringReadMusec.get();
    }

    /**
     * Get the cumulative value of the CPU time spent  by
     * the Hadoop Filesystem client while waiting for S3A to perform seek
     * operations. The time is in microseconds.
     *
     * @return cumulative elapsed time spend in write operations, in microseconds.
     */
    public static long getCPUTimeDuringSeekMusec() {
        return timeCPUDuringSeekMusec.get();
    }

    /**
     * Not yet implemented
     */
    public static long getTimeElapsedWriteMusec() {
        return timeElapsedWriteMusec.get();
    }

    /**
     * Increment the value of the cumulative elapsed time spent  by
     * Hadoop Filesystem clients waiting for S3A to return data of write
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public static void incrementTimeElapsedWriteOps(Long incrementTime) {
        timeElapsedWriteMusec.getAndAdd(incrementTime);
    }

    /**
     * Get the cumulative value of the number of bytes read  by
     * the Hadoop Filesystem client through the S3A connector
     *
     * @return cumulative bytes read with read operations.
     */
    public static long getBytesRead() {
        return bytesRead.get();
    }

    /**
     * Increment the value of the cumulative number of bytes read by
     * the Hadoop Filesystem clients reading from S3A
     *
     * @param incrementBytesRead number of bytes to add to the counter of bytes read.
     */
    public static void incrementBytesRead(Long incrementBytesRead) {
        bytesRead.getAndAdd(incrementBytesRead);
    }

    /**
     * Get the cumulative value of the number of bytes written  by
     * the Hadoop Filesystem client through the S3A connector
     *
     * @return cumulative bytes written via write operations.
     */
    public static long getBytesWritten() {
        return bytesWritten.get();
    }

    /**
     * Increment the value of the cumulative number of bytes read by
     * the Hadoop Filesystem clients reading from S3A
     *
     * @param incrementBytesWritten number of bytes to add to the counter of bytes read.
     */
    public static void incrementBytesWritten(Long incrementBytesWritten) {
        bytesWritten.getAndAdd(incrementBytesWritten);
    }

    /**
     * Get the cumulative value of the number of read operations performed by
     * the Hadoop Filesystem client through the S3A connector.
     *
     * @return cumulative number of read operations.
     */
    public static int getReadCalls() {
        return readCalls.get();
    }

    /**
     * Increment the counter of the cumulative number of read operations performed by
     * the Hadoop Filesystem clients reading from S3A.
     *
     * @param numOps increment the counter of read operations.
     */
    public static void incrementReadCalls(int numOps) {
        readCalls.getAndAdd(numOps);
    }

    public static int getSeekCalls() {
        return seekCalls.get();
    }
    public static void incrementSeekCalls(int numOps) {
        seekCalls.getAndAdd(numOps);
    }

    /**
     * Get the cumulative value of the number of write operations performed by
     * the Hadoop Filesystem client through the S3A connector.
     *
     * @return cumulative number of write operations.
     */
    public static int getWriteCalls() { return writeCalls.get(); }

    /**
     * Increment the counter of the cumulative number of write operations performed by
     * the Hadoop Filesystem clients writing to S3A.
     *
     * @param numOps increment the counter of write operations.
     */
    public static void incrementWriteCalls(int numOps) {
        writeCalls.getAndAdd(numOps);
    }
}
