package yang.plugin.tracing;

final class QueueElement {
	
	private int iCapacity = 40;
	private final int iCapInc = 40;
	private int iLast = -1;
	private int[] iarray = new int[iCapacity];
	
	int add(final int element) {
		if (++iLast == iCapacity) inccap();
		iarray[iLast] = element;
		return iLast;
	}
	
	private void inccap() {
		iCapacity += iCapInc;
		final int[] newarray = new int[iCapacity];
		for (int i=0; i<iLast; ++i) newarray[i] = iarray[i];
		iarray = newarray;
	}
	
	int get(final int index) { return iarray[index]; }
	
	int remove() { return iarray[iLast--]; }
	
	int remove(final int index) {
		final int i = iarray[index];
		iarray[index] = iarray[iLast--];
		return i;
	}
	
	int size() { return (iLast + 1); }
	
}