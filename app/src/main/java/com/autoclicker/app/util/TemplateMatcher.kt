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
            // Convert to grayscale for faster matching
            val sourceGray = toGrayscale(source)
            val templateGray = toGrayscale(template)
            
            // Perform normalized cross-correlation
            val matches = mutableListOf<MatchResult>()
            val searchWidth = source.width - template.width + 1
            val searchHeight = source.height - template.height + 1
            
            // Sliding window search
            for (y in 0 until searchHeight) {
                for (x in 0 until searchWidth) {
                    val confidence = computeNCC(
                        sourceGray, templateGray,
                        x, y,
                        template.width, template.height
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
     */
    private fun toGrayscale(bitmap: Bitmap): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val gray = IntArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            // Standard grayscale conversion formula
            gray[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        
        return gray
    }
    
    /**
     * Compute Normalized Cross-Correlation between template and source region.
     * 
     * NCC formula: sum((source[i] - mean_source) * (template[i] - mean_template)) / 
     *              (std_source * std_template * N)
     * 
     * @return Confidence value between -1.0 and 1.0 (normalized to 0.0-1.0)
     */
    private fun computeNCC(
        source: IntArray,
        template: IntArray,
        startX: Int,
        startY: Int,
        templateWidth: Int,
        templateHeight: Int
    ): Float {
        val sourceWidth = kotlin.math.sqrt(source.size.toDouble()).toInt()
        val n = templateWidth * templateHeight
        
        // Compute means
        var sourceMean = 0.0
        var templateMean = 0.0
        
        for (ty in 0 until templateHeight) {
            for (tx in 0 until templateWidth) {
                val sx = startX + tx
                val sy = startY + ty
                val sourceIdx = sy * sourceWidth + sx
                val templateIdx = ty * templateWidth + tx
                
                sourceMean += source[sourceIdx]
                templateMean += template[templateIdx]
            }
        }
        
        sourceMean /= n
        templateMean /= n
        
        // Compute standard deviations and cross-correlation
        var numerator = 0.0
        var sourceVar = 0.0
        var templateVar = 0.0
        
        for (ty in 0 until templateHeight) {
            for (tx in 0 until templateWidth) {
                val sx = startX + tx
                val sy = startY + ty
                val sourceIdx = sy * sourceWidth + sx
                val templateIdx = ty * templateWidth + tx
                
                val sourceDiff = source[sourceIdx] - sourceMean
                val templateDiff = template[templateIdx] - templateMean
                
                numerator += sourceDiff * templateDiff
                sourceVar += sourceDiff * sourceDiff
                templateVar += templateDiff * templateDiff
            }
        }
        
        // Avoid division by zero
        if (sourceVar == 0.0 || templateVar == 0.0) {
            return 0f
        }
        
        val denominator = sqrt(sourceVar * templateVar)
        val ncc = numerator / denominator
        
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

