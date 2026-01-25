package com.autoclicker.app.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

/**
 * Lightweight template matching implementation using Normalized Cross-Correlation.
 * 
 * This matcher finds occurrences of a template image within a larger source image
 * without requiring external libraries like OpenCV.
 * 
 * @author AutoClicker AI Module
 */
class TemplateMatcher {
    
    data class MatchResult(
        val x: Int,
        val y: Int,
        val confidence: Float
    )
    
    /**
     * Find all matches of template in source image.
     * 
     * @param source The source/screenshot bitmap to search in
     * @param template The template bitmap to find
     * @param threshold Confidence threshold (0.0 to 1.0), default 0.8
     * @param maxMatches Maximum number of matches to return, default 10
     * @return List of match results sorted by confidence (highest first)
     */
    fun match(
        source: Bitmap,
        template: Bitmap,
        threshold: Float = 0.8f,
        maxMatches: Int = 10
    ): List<MatchResult> {
        if (template.width > source.width || template.height > source.height) {
            CrashHandler.logWarning(
                "TemplateMatcher",
                "Template size (${template.width}x${template.height}) is larger than source (${source.width}x${source.height})"
            )
            return emptyList()
        }
        
        try {
            val sourceWidth = source.width
            val sourceHeight = source.height
            val templateWidth = template.width
            val templateHeight = template.height

            // Convert to grayscale for faster matching
            val sourceGray = toGrayscale(source)
            val templateGray = toGrayscale(template)
            
            // Pre-calculate template statistics to avoid redundant calculations in the sliding window
            val n = templateWidth * templateHeight
            var templateSum = 0.0
            var templateSumSq = 0.0
            for (gray in templateGray) {
                val g = gray.toDouble()
                templateSum += g
                templateSumSq += g * g
            }
            val templateMean = templateSum / n
            val templateSumSqDiff = templateSumSq - n * templateMean * templateMean

            // Perform normalized cross-correlation
            val matches = mutableListOf<MatchResult>()
            val searchWidth = sourceWidth - templateWidth + 1
            val searchHeight = sourceHeight - templateHeight + 1
            
            // Sliding window search
            for (y in 0 until searchHeight) {
                for (x in 0 until searchWidth) {
                    val confidence = computeNCC(
                        sourceGray, sourceWidth,
                        templateGray, templateWidth, templateHeight,
                        x, y,
                        templateMean, templateSumSqDiff
                    )
                    
                    if (confidence >= threshold) {
                        matches.add(MatchResult(x, y, confidence))
                    }
                }
            }
            
            // Apply non-maximum suppression to remove overlapping matches
            val filteredMatches = nonMaximumSuppression(
                matches,
                template.width,
                template.height
            )
            
            // Return top N matches sorted by confidence
            return filteredMatches
                .sortedByDescending { it.confidence }
                .take(maxMatches)
                
        } catch (e: Exception) {
            CrashHandler.logError("TemplateMatcher", "Error during template matching", e)
            return emptyList()
        }
    }
    
    /**
     * Convert bitmap to grayscale array for faster processing.
     * Optimized using bitwise operations and fixed-point arithmetic.
     */
    private fun toGrayscale(bitmap: Bitmap): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val size = width * height
        val pixels = IntArray(size)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val gray = IntArray(size)
        for (i in 0 until size) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Optimized formula: (77*R + 151*G + 28*B) / 256
            // This is faster than floating point math and avoids function calls
            gray[i] = (r * 77 + g * 151 + b * 28) shr 8
        }
        
        return gray
    }
    
    /**
     * Compute Normalized Cross-Correlation between template and source region.
     * Optimized to perform all calculations in a single pass using the algebraic formula:
     * sum((s - s_mean)(t - t_mean)) = sum(s*t) - n * s_mean * t_mean
     * 
     * @return Confidence value between -1.0 and 1.0 (normalized to 0.0-1.0)
     */
    private fun computeNCC(
        source: IntArray,
        sourceWidth: Int,
        template: IntArray,
        templateWidth: Int,
        templateHeight: Int,
        startX: Int,
        startY: Int,
        templateMean: Double,
        templateSumSqDiff: Double
    ): Float {
        val n = templateWidth * templateHeight
        
        var dotProduct = 0.0
        var sourceSum = 0.0
        var sourceSumSq = 0.0
        
        for (ty in 0 until templateHeight) {
            val sOffset = (startY + ty) * sourceWidth + startX
            val tOffset = ty * templateWidth
            for (tx in 0 until templateWidth) {
                val s = source[sOffset + tx].toDouble()
                val t = template[tOffset + tx].toDouble()
                
                dotProduct += s * t
                sourceSum += s
                sourceSumSq += s * s
            }
        }
        
        val sourceMean = sourceSum / n
        val sourceSumSqDiff = sourceSumSq - n * sourceMean * sourceMean
        
        // Avoid division by zero
        if (sourceSumSqDiff <= 0.0 || templateSumSqDiff <= 0.0) {
            return 0f
        }
        
        val numerator = dotProduct - n * sourceMean * templateMean
        val denominator = sqrt(sourceSumSqDiff * templateSumSqDiff)

        val ncc = (numerator / denominator).coerceIn(-1.0, 1.0)
        
        // Normalize to 0.0-1.0 range (NCC is in -1.0 to 1.0)
        return ((ncc + 1.0) / 2.0).toFloat()
    }
    
    /**
     * Non-maximum suppression to filter overlapping matches.
     * Keeps only the match with highest confidence in overlapping regions.
     */
    private fun nonMaximumSuppression(
        matches: List<MatchResult>,
        templateWidth: Int,
        templateHeight: Int
    ): List<MatchResult> {
        if (matches.isEmpty()) return emptyList()
        
        val sorted = matches.sortedByDescending { it.confidence }
        val kept = mutableListOf<MatchResult>()
        
        for (match in sorted) {
            var overlaps = false
            
            for (keptMatch in kept) {
                if (isOverlapping(
                        match.x, match.y, templateWidth, templateHeight,
                        keptMatch.x, keptMatch.y, templateWidth, templateHeight
                    )) {
                    overlaps = true
                    break
                }
            }
            
            if (!overlaps) {
                kept.add(match)
            }
        }
        
        return kept
    }
    
    /**
     * Check if two rectangles overlap.
     */
    private fun isOverlapping(
        x1: Int, y1: Int, w1: Int, h1: Int,
        x2: Int, y2: Int, w2: Int, h2: Int
    ): Boolean {
        return !(x1 + w1 < x2 || x2 + w2 < x1 || y1 + h1 < y2 || y2 + h2 < y1)
    }
    
    companion object {
        private var instance: TemplateMatcher? = null
        
        fun getInstance(): TemplateMatcher {
            if (instance == null) {
                instance = TemplateMatcher()
            }
            return instance!!
        }
    }
}

