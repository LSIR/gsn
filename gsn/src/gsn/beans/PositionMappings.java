package gsn.beans;

import java.util.ArrayList;
import java.util.Iterator;

public class PositionMappings {
	public Integer position;
	public ArrayList<PositionMap> mappings;
	
	public PositionMappings() {}
	
	public PositionMappings(Integer position, ArrayList<PositionMap> mappings) {
		this.position = position;
		this.mappings = mappings;
	}

	public void add(PositionMap positionMap) {
		Iterator<PositionMap> iter = mappings.iterator();
		while (iter.hasNext()) {
			PositionMap map = iter.next();
			if (map.end == null && map.begin.compareTo(positionMap.begin) == 0)
				iter.remove();
		}
		mappings.add(positionMap);
	}
}
