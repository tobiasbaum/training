package de.set.trainingUI;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class DiagramData {

	public static CategoryDataset getTasksPerWeek(Trainee t) {
		final Map<String, List<Trial>> trialsPerWeek = getTrialsPerWeek(t);
    	final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    	for (final Entry<String, List<Trial>> e : trialsPerWeek.entrySet()) {
    		dataset.addValue(e.getValue().size(), "", e.getKey());
    	}
		return dataset;
	}

	private static Map<String, List<Trial>> getTrialsPerWeek(Trainee trainee) {
		final Map<String, List<Trial>> ret = new TreeMap<>();
		for (final Trial trial : trainee.getTrials()) {
			final String week = getWeek(trial);
			List<Trial> list = ret.get(week);
			if (list == null) {
				list = new ArrayList<>();
				ret.put(week, list);
			}
			list.add(trial);
		}
		return ret;
	}

	private static String getWeek(Trial trial) {
		final ZonedDateTime zdt = ZonedDateTime.ofInstant(trial.getStartTime(), ZoneId.systemDefault());
		return String.format("%2d/%d", zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), zdt.getYear());
	}

}
