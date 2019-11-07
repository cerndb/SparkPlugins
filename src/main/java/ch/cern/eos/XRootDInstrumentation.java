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
package ch.cern.eos;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class XRootDInstrumentation {

    private static AtomicLong timeElapsedReadMusec = new AtomicLong();
    private static AtomicLong timeElapsedWriteMusec = new AtomicLong();
    private static AtomicInteger readOps = new AtomicInteger();
    private static AtomicInteger writeOps = new AtomicInteger();
    private static AtomicLong bytesRead = new AtomicLong();
    private static AtomicLong bytesWritten = new AtomicLong();

    /**
     * Get the cumulative value of the elapsed time spent  by
     * the Hadoop Filesystem client waiting for EOS/XRootD to return data of read
     * operations. The time is in microseconds.
     * Use from Hadoop clients, such as Spark environments, by calling
     * ch.cern.eos.XRootDInstrumentation.getTimeElapsedReadMusec()
     *
     * @return cumulative elapsed time spend in read operations, in microseconds.
     */
    public static long getTimeElapsedReadMusec() {
        return timeElapsedReadMusec.get();
    }

    /**
     * Increment the value of the cumulative elapsed time spent  by
     * Hadoop Filesystem clients waiting for EOS/XRootD to return data of read
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public void incrementTimeElapsedReadOps(Long incrementTime) {
        timeElapsedReadMusec.getAndAdd(incrementTime);
    }

    /**
     * Get the cumulative value of the elapsed time spent  by
     * the Hadoop Filesystem client waiting for EOS/XRootD to perform write
     * operations. The time is in microseconds.
     *
     * @return cumulative elapsed time spend in write operations, in microseconds.
     */
    public static long getTimeElapsedWriteMusec() {
        return timeElapsedWriteMusec.get();
    }

    /**
     * Increment the value of the cumulative elapsed time spent  by
     * Hadoop Filesystem clients waiting for EOS/XRootD to return data of write
     * operations. The time is in microseconds.
     *
     * @param incrementTime the time to add to the cumulative value, in microseconds
     */
    public void incrementTimeElapsedWriteOps(Long incrementTime) {
        timeElapsedWriteMusec.getAndAdd(incrementTime);
    }

    /**
     * Get the cumulative value of the number of bytes read  by
     * the Hadoop Filesystem client through the XRootD connector
     *
     * @return cumulative bytes read with read operations.
     */
    public static long getBytesRead() {
        return bytesRead.get();
    }

    /**
     * Increment the value of the cumulative number of bytes read by
     * the Hadoop Filesystem clients reading from EOS/XRootD
     *
     * @param incrementBytesRead number of bytes to add to the counter of bytes read.
     */
    public void incrementBytesRead(Long incrementBytesRead) {
        bytesRead.getAndAdd(incrementBytesRead);
    }

    /**
     * Get the cumulative value of the number of bytes written  by
     * the Hadoop Filesystem client through the XRootD connector
     *
     * @return cumulative bytes written via write operations.
     */
    public static long getBytesWritten() {
        return bytesWritten.get();
    }

    /**
     * Increment the value of the cumulative number of bytes read by
     * the Hadoop Filesystem clients reading from EOS/XRootD
     *
     * @param incrementBytesWritten number of bytes to add to the counter of bytes read.
     */
    public void incrementBytesWritten(Long incrementBytesWritten) {
        bytesWritten.getAndAdd(incrementBytesWritten);
    }

    /**
     * Get the cumulative value of the number of read operations performed by
     * the Hadoop Filesystem client through the XRootD connector.
     *
     * @return cumulative number of read operations.
     */
    public static int getReadOps() {
        return readOps.get();
    }

    /**
     * Increment the counter of the cumulative number of read operations performed by
     * the Hadoop Filesystem clients reading from EOS/XRootD.
     *
     * @param numOps increment the counter of read operations.
     */
    public void incrementReadOps(int numOps) {
        readOps.getAndAdd(numOps);
    }

    /**
     * Get the cumulative value of the number of write operations performed by
     * the Hadoop Filesystem client through the XRootD connector.
     *
     * @return cumulative number of write operations.
     */
    public static int getWriteOps() { return writeOps.get(); }

    /**
     * Increment the counter of the cumulative number of write operations performed by
     * the Hadoop Filesystem clients writing to EOS/XRootD.
     *
     * @param numOps increment the counter of write operations.
     */
    public void incrementWriteOps(int numOps) {
        writeOps.getAndAdd(numOps);
    }
}
