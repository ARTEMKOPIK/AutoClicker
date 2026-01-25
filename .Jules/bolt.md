# Bolt's Journal - Critical Learnings

## 2026-01-25 - Template Matching Performance
**Learning:** Sliding window template matching with Normalized Cross-Correlation (NCC) is extremely expensive (O(N*M*n*m)). Standard implementations often redundantly calculate mean and variance for every window position, and sometimes contain inefficient coordinate mapping (e.g., using `sqrt` on array size to find width).
**Action:** Always pre-calculate template statistics, use actual dimensions instead of heuristics for coordinate mapping, and use the single-pass algebraic formula for NCC to halve the number of operations in the inner loop.
