package com.example.data

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

object VideoTrimmerHelper {
    private const val TAG = "VideoTrimmerHelper"

    fun trimMp4(sourceFile: File, outputFile: File, startMs: Long, endMs: Long): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(sourceFile.absolutePath)

            val trackCount = extractor.trackCount
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackIndexMap = HashMap<Int, Int>()
            var maxBufferSize = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                
                // Only select video and audio tracks
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    trackIndexMap[i] = dstIndex

                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val inputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (inputSize > maxBufferSize) {
                            maxBufferSize = inputSize
                        }
                    }
                }
            }

            if (maxBufferSize <= 0) {
                maxBufferSize = 1024 * 1024 // Fallback 1MB buffer
            }

            muxer.start()

            val startUs = startMs * 1000
            val endUs = endMs * 1000

            // Seek to start position
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            var firstFrameTimeUs = -1L

            while (true) {
                val sampleTrackIndex = extractor.sampleTrackIndex
                if (sampleTrackIndex < 0) {
                    break // End of stream
                }

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endUs) {
                    break // Beyond the requested crop end
                }

                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }

                if (firstFrameTimeUs == -1L) {
                    firstFrameTimeUs = presentationTimeUs
                }

                bufferInfo.presentationTimeUs = presentationTimeUs - firstFrameTimeUs
                bufferInfo.flags = extractor.sampleFlags

                val dstTrackIndex = trackIndexMap[sampleTrackIndex]
                if (dstTrackIndex != null) {
                    muxer.writeSampleData(dstTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming video", e)
            return false
        } finally {
            try {
                extractor?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing extractor", e)
            }
            try {
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing muxer", e)
            }
        }
    }
}
