package com.dburnwal.inspectionassistant.vision;

import com.dburnwal.inspectionassistant.inspection.Finding;

import java.util.List;

public interface VisionAnalyzer {
    List<Finding> analyze(byte[] imageBytes);
}
