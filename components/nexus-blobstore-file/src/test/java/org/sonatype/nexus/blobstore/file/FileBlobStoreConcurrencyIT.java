/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.blobstore.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.file.internal.MetricsInputStream;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.io.ByteStreams.nullOutputStream;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;

/**
 * {@link FileBlobStore} concurrency tests.
 */
public class FileBlobStoreConcurrencyIT
    extends TestSupport
{
  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin"
  );

  public static final int BLOB_MAX_SIZE_BYTES = 5_000_000;

  private BlobMetadataStore metadataStore;

  private FileBlobStore underTest;

  @Before
  public void setUp() throws Exception {
    Path root = util.createTempDir().toPath();
    Path content = root.resolve("content");
    Path metadata = root.resolve("metadata");

    this.metadataStore = MapdbBlobMetadataStore.create(metadata.toFile());
    this.underTest = new FileBlobStore(content, new VolumeChapterLocationStrategy(), new SimpleFileOperations(), metadataStore,
        new BlobStoreConfiguration());
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();
  }

  @Test
  public void concurrencyTest() throws Exception {

    final Random random = new Random();

    List<TestWorker> testWorkers = new ArrayList<TestWorker>();

    int numberOfCreators = 10;
    int numberOfDeleters = 5;
    int numberOfReaders = 30;
    int numberOfCompactors = 1;
    int numberOfShufflers = 3;

    int numberOfIterations = 15;

    int numberOfThreads =
        numberOfCreators + numberOfDeleters + numberOfReaders + numberOfShufflers + numberOfCompactors;

    // A cyclic barrier is used to increase contention between the threads.
    final CyclicBarrier startingGun = new CyclicBarrier(numberOfThreads, new Runnable()
    {
      int iterationCount = 0;

      @Override
      public void run() {
        log("Iteration starting: " + iterationCount++);
      }
    });

    final Queue<BlobId> blobIdsInTheStore = new ConcurrentLinkedDeque<>();

    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);

    // A signal to get test workers to abort if a peer finds a failure
    final AtomicBoolean failureHasOccurred = new AtomicBoolean();
    failureHasOccurred.set(false);

    for (int i = 0; i < numberOfCreators; i++) {
      TestWorker r = new TestWorker(testWorkers.size(), startingGun, failureHasOccurred, numberOfIterations,
          new TestTask()
          {
            @Override
            public void run() throws Exception {
              final byte[] data = new byte[random.nextInt(BLOB_MAX_SIZE_BYTES) + 1];
              random.nextBytes(data);
              final Blob blob = underTest.create(new ByteArrayInputStream(data), TEST_HEADERS);

              blobIdsInTheStore.add(blob.getId());
            }
          }
      );
      testWorkers.add(r);
    }

    for (int i = 0; i < numberOfReaders; i++) {
      TestWorker r = new TestWorker(testWorkers.size(), startingGun, failureHasOccurred, numberOfIterations,
          new TestTask()
          {
            @Override
            public void run() throws Exception {
              final BlobId blobId = blobIdsInTheStore.peek();

              log("Attempting to read " + blobId);

              if (blobId == null) {
                return;
              }

              final Blob blob = underTest.get(blobId);
              if (blob == null) {
                log("Attempted to obtain blob, but it was deleted:" + blobId);
                return;
              }

              try (InputStream inputStream = blob.getInputStream()) {
                readContentAndValidateMetrics(blobId, inputStream, blob.getMetrics());
              }
              catch (BlobStoreException e) {
                // This is normal operation if another thread deletes your blob after you obtain a Blob reference
                log("Concurrent deletion suspected while calling blob.getInputStream().", e);
              }
            }
          }
      );
      testWorkers.add(r);
    }

    for (int i = 0; i < numberOfDeleters; i++) {
      testWorkers
          .add(new TestWorker(testWorkers.size(), startingGun, failureHasOccurred, numberOfIterations, new TestTask()
          {
            @Override
            public void run() throws Exception {
              final BlobId blobId = blobIdsInTheStore.poll();
              if (blobId == null) {
                log("deleter: null blob id");
                return;
              }
              underTest.delete(blobId);
            }
          }));
    }

    // Shufflers pull blob IDs off the front of the queue and stick them on the back, to make the blobID queue a bit less orderly
    for (int i = 0; i < numberOfShufflers; i++) {
      testWorkers.add(
          new TestWorker(testWorkers.size(), startingGun, failureHasOccurred, numberOfIterations, new TestTask()
          {
            @Override
            public void run() throws Exception {
              final BlobId blobId = blobIdsInTheStore.poll();
              if (blobId != null) {
                blobIdsInTheStore.add(blobId);
              }
            }
          }));
    }

    for (int i = 0; i < numberOfCompactors; i++) {
      testWorkers.add(
          new TestWorker(testWorkers.size(), startingGun, failureHasOccurred, numberOfIterations, new TestTask()
          {
            @Override
            public void run() throws Exception {
              underTest.compact();
            }
          }));
    }

    //Collections.shuffle(testWorkers);
    for (Runnable r : testWorkers) {
      service.submit(r);
    }

    service.shutdown();
    service.awaitTermination(5, TimeUnit.MINUTES);

    for (TestWorker testWorker : testWorkers) {
      if (testWorker.experiencedFailure()) {
        fail("A test worker experienced a failure.");
      }
    }
  }

  /**
   * Read all the content from a blob, and compare it with the metrics on file in the blob store.
   *
   * @throws RuntimeException if there is any deviation
   */
  private void readContentAndValidateMetrics(final BlobId blobId,
                                             final InputStream inputStream,
                                             final BlobMetrics metadataMetrics)
      throws NoSuchAlgorithmException, IOException
  {
    final MetricsInputStream measured = new MetricsInputStream(inputStream);
    ByteStreams.copy(measured, nullOutputStream());

    checkEqual("stream length", metadataMetrics.getContentSize(), measured.getSize(), blobId);
    checkEqual("SHA1 hash", metadataMetrics.getSHA1Hash(), measured.getMessageDigest(), blobId);
  }

  private void checkEqual(final String propertyName, final Object expected, final Object measured,
                          final BlobId blobId)
  {
    if (!Objects.equal(measured, expected)) {
      throw new RuntimeException(
          "Blob " + blobId + "'s measured " + propertyName + " differed from its metadata. Expected " + expected +
              " but was " + measured + "."
      );
    }
  }

  private interface TestTask
  {
    void run() throws Exception;
  }

  /**
   * A helper class to wrap a TestTask and trap any exceptions that it throws. If an exception is thrown, it
   * trips a flag to force its peers to abort sooner, rather than waiting for the executor service to time out.
   */
  private class TestWorker
      implements Runnable
  {

    private final CyclicBarrier iterationStartSignal;

    private final AtomicBoolean failureSignal;

    private final int iterations;

    private final int workerNumber;

    private final TestTask testTask;

    private final AtomicReference<Exception> exception = new AtomicReference<>();

    private TestWorker(final int workerNumber, final CyclicBarrier iterationStartSignal,
                       final AtomicBoolean failureSignal,
                       final int iterations,
                       final TestTask testTask)
    {
      this.testTask = testTask;
      this.iterationStartSignal = iterationStartSignal;
      this.failureSignal = failureSignal;
      this.iterations = iterations;
      this.workerNumber = workerNumber;
    }

    @Override
    public void run() {
      try {
        for (int i = 0; i < iterations; i++) {
          if (failureSignal.get()) {
            log("Test worker " + workerNumber + " aborting before iteration " + i);
            break;
          }
          iterationStartSignal.await();
          testTask.run();
          log("Test worker " + workerNumber + " completing iteration " + i);
        }
      }
      catch (Exception e) {
        failureSignal.set(true);
        // Dislodge any peers waiting at the barrier
        iterationStartSignal.reset();
        this.exception.set(e);
        log("Runnable threw an exception for worker " + workerNumber, e);
      }
      finally {
        log("Test worker " + workerNumber + " completing.");
      }
    }

    public boolean experiencedFailure() {
      return exception.get() != null;
    }

    public Exception getException() {
      return exception.get();
    }
  }

}
