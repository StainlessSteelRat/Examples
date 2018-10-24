package io.echoplex.web.service.data;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import io.echoplex.web.beans.PointF;

@Service
public class PointsMapService {
	
	Map<String, PointF> points;
	
	@PostConstruct
	public void init() {
		points = new HashMap<>();
	}

	public Map<String, PointF> getPoints() {
		return points;
	}
}
