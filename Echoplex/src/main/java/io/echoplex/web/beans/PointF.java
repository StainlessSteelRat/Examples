package io.echoplex.web.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointF {
	final Logger log = LoggerFactory.getLogger(PointF.class);
	
	public double x;
	public double y;
	
	public PointF(float x, float y) {
		this.x = x;
		this.y = y;
		log.info("[" + x + "," + y +"]");
	}
}