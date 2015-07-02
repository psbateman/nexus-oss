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
package org.sonatype.nexus.atlas.internal

import com.google.common.io.CountingOutputStream
import org.sonatype.nexus.supportzip.SupportBundle
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type
import org.sonatype.nexus.supportzip.SupportBundleCustomizer
import org.sonatype.nexus.supportzip.SupportZipGenerator
import org.sonatype.nexus.supportzip.SupportZipGenerator.Request
import org.sonatype.nexus.supportzip.SupportZipGenerator.Result
import org.sonatype.nexus.wonderland.DownloadService
import org.sonatype.sisu.goodies.common.ByteSize
import org.sonatype.sisu.goodies.common.ComponentSupport

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import java.util.zip.Deflater
import java.util.zip.ZipEntry

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.CONFIG
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.JMX
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.LOG
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.METRICS
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.SECURITY
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.SYSINFO
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.THREAD

/**
 * Default {@link SupportZipGenerator}.
 *
 * @since 2.7
 */
@Named
@Singleton
class SupportZipGeneratorImpl
    extends ComponentSupport
    implements SupportZipGenerator
{
  private final List<SupportBundleCustomizer> bundleCustomizers

  private final DownloadService downloadService

  /**
   * The maximum (uncompressed) size of any one file that is included into the ZIP file when limit files is enabled.
   */
  private final ByteSize maxFileSize

  /**
   * The maximum (compressed) size of the entire ZIP file when limit ZIP size is enabled.
   */
  private final ByteSize maxZipFileSize

  @Inject
  SupportZipGeneratorImpl(final DownloadService downloadService,
                          final List<SupportBundleCustomizer> bundleCustomizers,
                          final @Named('${atlas.supportZipGenerator.maxFileSize:-30mb}') ByteSize maxFileSize,
                          final @Named('${atlas.supportZipGenerator.maxZipFileSize:-20mb}') ByteSize maxZipFileSize)
  {
    assert downloadService
    this.bundleCustomizers = checkNotNull(bundleCustomizers)

    this.downloadService = downloadService

    this.maxFileSize = maxFileSize
    log.info 'Maximum included file size: {}', maxFileSize

    this.maxZipFileSize = maxZipFileSize
    log.info 'Maximum ZIP file size: {}', maxZipFileSize
  }

  /**
   * Return set of included content source types.
   */
  private Set<Type> includedTypes(final Request request) {
    def types = []
    if (request.systemInformation) {
      types << SYSINFO
    }
    if (request.threadDump) {
      types << THREAD
    }
    if (request.metrics) {
      types << METRICS
    }
    if (request.configuration) {
      types << CONFIG
    }
    if (request.security) {
      types << SECURITY
    }
    if (request.log) {
      types << LOG
    }
    if (request.jmx) {
      types << JMX
    }
    return types
  }

  /**
   * Filter only included content sources.
   */
  private List<ContentSource> filterSources(final Request request, final SupportBundle supportBundle) {
    def include = includedTypes(request)
    def sources = []
    supportBundle.sources.each {
      if (include.contains(it.type)) {
        log.debug 'Including content source: {}', it
        sources << it
      }
    }
    return sources
  }

  @Override
  Result generate(final Request request) {
    assert request

    log.info 'Generating support ZIP: {}', request

    def bundle = new SupportBundle()

    // customize the bundle
    bundleCustomizers.each {
      log.debug 'Customizing bundle with: {}', it
      it.customize(bundle)
    }
    assert !bundle.sources.isEmpty(): 'At least one bundle source must be configured'

    // filter only sources which user requested
    def sources = filterSources(request, bundle)
    assert !sources.isEmpty(): 'At least one content source must be configured'

    try {
      // prepare bundle sources
      sources.each {
        log.debug 'Preparing bundle source: {}', it
        it.prepare()
      }

      return createZip(request, sources)
    }
    catch (Exception e) {
      log.error 'Failed to create support ZIP', e
    }
    finally {
      // cleanup bundle sources
      sources.each {
        log.debug 'Cleaning bundle source: {}', it
        try {
          it.cleanup()
        }
        catch (Exception e) {
          log.warn 'Bundle source cleanup failed', e
        }
      }
    }
  }

  /**
   * Create a ZIP file with content from given sources.
   */
  private Result createZip(final Request request, final List<ContentSource> sources) {
    def prefix = downloadService.uniqueName('support-')

    // Write zip to temporary file first
    def file = File.createTempFile("${prefix}-", '.zip').canonicalFile
    log.debug 'Writing ZIP file: {}', file

    // track total compressed and uncompressed size
    def stream = new CountingOutputStream(file.newOutputStream())
    long totalUncompressed = 0

    // setup zip too sync-flush so we can detect compressed size for partially written files
    def zip = new FlushableZipOutputStream(stream)
    zip.level = Deflater.DEFAULT_COMPRESSION
    zip.syncFlush = true

    def percentCompressed = { long compressed, long uncompressed ->
      100 - ((compressed / uncompressed) * 100) as int
    }

    // helper to create normalized entry with prefix
    def addEntry = { String path ->
      if (!path.startsWith('/')) {
        path = '/' + path
      }
      def entry = new ZipEntry(prefix + path)
      zip.putNextEntry(entry)
      return entry
    }

    // helper to close entry
    def closeEntry = { ZipEntry entry ->
      zip.closeEntry()
      // not all entries have a size
      if (entry.size) {
        if (log.debugEnabled) {
          log.debug 'Entry (in={} out={}) bytes, compressed: {}%',
              entry.size,
              entry.compressedSize,
              percentCompressed(entry.compressedSize, entry.size)
        }
        totalUncompressed += entry.size
      }
    }

    // helper to add entries for each directory
    def addDirectoryEntries = {
      // include entry for top-level directory
      addEntry '/'

      // add unique directory entries
      Set<String> dirs = []
      sources.each {
        def path = it.path.split('/') as List
        if (path.size() > 1) {
          // eg. 'foo/bar/baz' -> [ 'foo', 'foo/bar' ]
          for (int l = path.size(); l > 1; l--) {
            dirs << path[0..-l].join('/')
          }
        }
      }
      dirs.sort().each {
        log.debug 'Adding directory entry: {}', it
        def entry = addEntry "${it}/"
        // must end with '/'
        closeEntry entry
      }
    }

    // maximum size of included content
    final int maxContentSize = this.maxFileSize.toBytes()

    // size of chunks for appending source content and detecting max ZIP size
    final int chunkSize = 4 * 1024

    // leave some fudge room so we can close the zip file and write marker tokens if needed
    final int maxZipSize = maxZipFileSize.toBytes() - (chunkSize * 2)

    // token added to files to indicate truncation has occurred
    final String TRUNCATED_TOKEN = '** TRUNCATED **'

    // flag to indicate if any content was truncated
    boolean truncated = false

    try {
      // add directory entries
      addDirectoryEntries()

      // TODO: Sort out how to deal with obfuscation, if its specific or general
      // TODO: ... this should be a detail of the content source

      // add content entries, sorted so highest priority are processed first
      sources.sort().each { source ->
        log.debug 'Adding content entry: {} {} bytes', source, source.size
        def entry = addEntry source.path

        source.content.withStream { InputStream input ->
          // truncate content which is larger than maximum file size
          if (request.limitFileSizes && source.size > maxContentSize) {
            log.warn 'Truncating source contents; exceeds maximum included file size: {}', source.path
            zip << TRUNCATED_TOKEN
            truncated = true
            input.skip(source.size - maxContentSize)
          }

          // write source content to the zip stream in chunks
          byte[] buff = new byte[chunkSize]
          int len
          while ((len = input.read(buff)) != -1) {
            // truncate content if max ZIP size reached
            if (request.limitZipSize && stream.count + len > maxZipSize) {
              log.warn 'Truncating source contents; max ZIP size reached: {}', source.path
              zip << TRUNCATED_TOKEN
              truncated = true
              break
            }

            zip.write buff, 0, len

            // flush so we can detect compressed size for partially written files
            zip.flush()
          }
        }

        closeEntry entry
      }

      // add marker to top of file if we truncated anything
      if (truncated) {
        addEntry 'truncated'
      }
    }
    finally {
      zip.close()
    }

    if (log.debugEnabled) {
      log.debug 'ZIP file (in={} out={}) bytes, compressed: {}%',
          totalUncompressed,
          stream.count,
          percentCompressed(stream.count, totalUncompressed)
    }

    // move the file into place
    def target = downloadService.move(file, "${prefix}.zip")
    log.info 'Created support ZIP file: {}', target

    return new Result(
        file: target,
        truncated: truncated
    )
  }
}