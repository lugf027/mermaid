/** A single diagram sample from SampleData */
export interface DiagramSample {
  name: string;
  type: string;
  text: string;
}

/** Screenshot comparison result for a single sample */
export interface CompareResult {
  sample: DiagramSample;
  kmpScreenshot: string;  // file path to kmp screenshot
  jsScreenshot: string;   // file path to js screenshot
  diffImage: string;      // file path to diff image
  diffPixels: number;     // number of different pixels
  totalPixels: number;    // total number of pixels
  diffPercentage: number; // percentage of different pixels (0-100)
  status: 'pass' | 'fail' | 'error';
  errorMessage?: string;
}

/** Full comparison report */
export interface CompareReport {
  timestamp: string;
  baseUrl: string;
  results: CompareResult[];
  summary: {
    total: number;
    passed: number;
    failed: number;
    errors: number;
    avgDiffPercentage: number;
  };
}
