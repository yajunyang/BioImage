package yang.plugin.tracing;

final class Values {
	
	private int capacity = 1000;
	private final int capinc = 1000;
	private int size = 0;
	private double[] varray = new double[capacity];
	private double sum, mean, sd, min, max;
	
	void add(final double value) {
		if (size == capacity) inccap();
		varray[size++] = value;
	}
	
	private void inccap() {
		capacity += capinc;
		final double[] newarray = new double[capacity];
		for (int i=0; i<size; ++i) newarray[i] = varray[i];
		varray = newarray;
	}
	
	void reset() { size = 0; }
	
	void stats() {
		
		if (size == 0) {
			sum = mean = sd = min = max = 0;
		} else {
			double val = 0;
			sum = min = max = varray[0];
			for (int i=1; i<size; ++i) {
				val = varray[i];
				sum += val;
				if (val < min) min = val;
				else if (val > max) max = val;
			}
			mean = sum/size;
			double sumdev2 = 0;
			for (int i=0; i<size; ++i) {
				val = varray[i] - mean;
				sumdev2 += val*val;
			}
			sd = Math.sqrt(sumdev2/(size-1));
		}
	}
	
	int count() { return size; }
	
	double sum() { return sum; }
	
	double mean() { return mean; }
	
	double sd() { return sd; }
	
	double min() { return min; }
	
	double max() { return max; }
	
}