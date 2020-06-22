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

import java.util.concurrent.atomic.AtomicLong;

public class S3ATimeInstrumentation {

    private static AtomicLong timeElapsedReadMusec = new AtomicLong();
    private static AtomicLong timeElapsedSeekMusec = new AtomicLong();
    private static AtomicLong timeCPUDuringReadMusec = new AtomicLong();
    private static AtomicLong timeCPUDuringSeekMusec = new AtomicLong();
    private static AtomicLong timeGetObjectMetadata = new AtomicLong();
    private static AtomicLong timeCPUGetObjectMetadata = new AtomicLong();
    private static AtomicLong bytesRead = new AtomicLong();

    /**
     * Increment the value of the cumulative elapsed time spent in read operations.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds.
     */
    public static void incrementTimeElapsedReadOps(Long incrementTime) {
        timeElapsedReadMusec.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative CPU time spent in read operations.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds.
     */
    public static void incrementCPUTimeDuringRead(Long incrementTime) {
        timeCPUDuringReadMusec.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative CPU time spent in seek operations.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds.
     */
    public static void incrementCPUTimeDuringSeek(Long incrementTime) {
        timeCPUDuringSeekMusec.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative elapsed time spent in seek operations.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds.
     */
    public static void incrementTimeElapsedSeekOps(Long incrementTime) {
        timeElapsedSeekMusec.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative elapsed time spent in getObjectMetadata operations.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds.
     */
    public static void incrementTimeGetObjectMetadata(Long incrementTime) {
        timeGetObjectMetadata.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative CPU time spent in getObjectMetadata operations.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds.
     */
    public static void incrementTimeCPUGetObjectMetadata(Long incrementTime) {
        timeCPUGetObjectMetadata.getAndAdd(incrementTime);
    }

    /**
     * Increment the value of the cumulative bytes read..
     *
     * @param incrementBytesRead bytes to add to the cumulative value.
     */

    public static void incrementBytesRead(Long incrementBytesRead) {
        bytesRead.getAndAdd(incrementBytesRead);
    }

    /**
     * Get the cumulative value of the elapsed time spent in seek operations.
     *
     * @return cumulative elapsed time spent in read operations, in microseconds.
     */
    public static long getTimeElapsedSeekMusec() {
        return timeElapsedSeekMusec.get();
    }

    /**
     * Get the cumulative value of the CPU time spent in read operations.
     *
     * @return cumulative CPU time spent in read operations, in microseconds.
     */
    public static long getCPUTimeDuringReadMusec() {
        return timeCPUDuringReadMusec.get();
    }

    /**
     * Get the cumulative value of the elapsed time spent in read operations.
     *
     * @return cumulative elapsed time spent in read operations, in microseconds.
     */
    public static long getTimeElapsedReadMusec() {
        return timeElapsedReadMusec.get();
    }

    /**
     * Get the cumulative value of the CPU time spent in seek operations.
     *
     * @return cumulative CPU time spent in seek operations, in microseconds.
     */
    public static long getCPUTimeDuringSeekMusec() {
        return timeCPUDuringSeekMusec.get();
    }

    /**
     * Get the cumulative value of the time spent in getObjectMetadata operations.
     *
     * @return cumulative time spent in getObjectMetadata operations, in microseconds.
     */
    public static long getTimeGetObjectMetadata() {
        return timeGetObjectMetadata.get();
    }

    /**
     * Get the cumulative value of the CPU time spent in getObjectMetadata operations.
     *
     * @return cumulative CPU time spent in getObjectMetadata operations, in microseconds.
     */
    public static long getTimeCPUGetObjectMetadata() {
        return timeCPUGetObjectMetadata.get();
    }

    /**
     * Get the cumulative value of bytes read.
     *
     * @return cumulative bytes read by S3AInputStream (redundant with standard instrumentation,
     * it is there for diagnostic purposes).
     */
    public static long getBytesRead() {
        return bytesRead.get();
    }

}
