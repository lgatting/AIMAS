package searchclient;

import java.util.Comparator;

public class GoalPriorityComparator implements Comparator<int[]> {
	
	@Override
	public int compare(int[] o1, int[] o2) {
		if(o1.length < 3 || o2.length < 3){
			throw new IndexOutOfBoundsException("Cannot compare priorities due to out of bounds");
		}
		return o1[2] - o2[2];
	}
}
