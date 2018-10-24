package io.echoplex.web.ui.components;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.tapio.googlemaps.GoogleMap;
import com.vaadin.tapio.googlemaps.client.GoogleMapControl;
import com.vaadin.tapio.googlemaps.client.LatLon;
import com.vaadin.tapio.googlemaps.client.events.MarkerDragListener;
import com.vaadin.tapio.googlemaps.client.overlays.GoogleMapInfoWindow;
import com.vaadin.tapio.googlemaps.client.overlays.GoogleMapMarker;

import io.echoplex.web.beans.Echo;
import io.echoplex.web.beans.PointF;
import io.echoplex.web.service.data.PointsMapService;
import io.echoplex.web.utils.OpenInfoWindowOnMarkerClickListener;

public class HaloMap extends GoogleMap implements Runnable {
	private static final long serialVersionUID = -686352637627944417L;
	private static HaloMap HALOMAP;
	private static Logger log = LoggerFactory.getLogger(HaloMap.class);
	
	public static HaloMap get() { return HALOMAP; }
	
	private Map<GoogleMapMarker, Echo> markersMapByMarker = new HashMap<>();
	private Map<Echo, GoogleMapMarker> markersMapByShop = new HashMap<>();
	
	private Map<String, GoogleMapMarker> markersMapByImei = new HashMap<>();
	private Map<GoogleMapMarker, String> markersMapByMarkerImei = new HashMap<>();
	
	@Autowired 
	private PointsMapService points;

	
	
	public HaloMap(String apiKey, String clientId, String language) {
		super(apiKey, clientId, language);
		HALOMAP = this;
		
		Set<GoogleMapControl> controls = new HashSet<GoogleMapControl>();
		controls.add(GoogleMapControl.Pan);
		controls.add(GoogleMapControl.Rotate);
		controls.add(GoogleMapControl.Zoom);
		controls.add(GoogleMapControl.Scale);

		this.setStyleName("google-map");
		this.setCenter(new LatLon(60.440963, 22.25122));
		this.setZoom(15);
		this.setSizeFull();
		this.setControls(controls);
		
		
		this.addMarkerDragListener(new MarkerDragListener() {
			private static final long serialVersionUID = 531477711780108628L;
			@Override
			public void markerDragged(GoogleMapMarker draggedMarker, LatLon oldPosition) {
				log.info("Marker \"" + draggedMarker.getCaption() + "\" dragged from (" + oldPosition.getLat() + ", "
						+ oldPosition.getLon() + ") to (" + draggedMarker.getPosition().getLat() + ", " + draggedMarker.getPosition().getLon() + ")");
				
				//Update Shop
				Echo echo = markersMapByMarker.get(draggedMarker);
				echo.setLat(draggedMarker.getPosition().getLat());
				echo.setLng(draggedMarker.getPosition().getLon());
				// Save shop
				//UI.getCurrent().getDAO().save(shop);
			}
		});
		
		
		
		Thread th = new Thread(this);
		th.start();
	}
	
	/*
	 * kakolaMarker.setAnimationEnabled(true);
		googleMap.addMarker(kakolaMarker);
		googleMap.addMarker("DRAGGABLE: Paavo Nurmi Stadion", new LatLon(60.442423, 22.26044), true, "VAADIN/1377279006_stadium.png");
		googleMap.addMarker("NOT DRAGGABLE: Iso-Heikkil�", new LatLon(60.450403, 22.230399), false, null);
		googleMap.setMinZoom(4);
		googleMap.setMaxZoom(16);
		
		kakolaInfoWindow.setWidth("400px");
		kakolaInfoWindow.setHeight("500px");
	 */
	
	public void populateMapWithEchoes(List<Echo> list) {
		for (Echo echo : list) {
			addStore(echo);
		}		
	}
	
	public void addStore(Echo echo) {
		GoogleMapMarker marker = new GoogleMapMarker(echo.getOwner(), new LatLon(echo.getLat(), echo.getLng()), true, "VAADIN/themes/haloEarth/img/appicon24r.png"); //"VAADIN/themes/haloEarth/img/appicon60.png");
		marker.setAnimationEnabled(true);		
		
		this.addMarker(marker);
		final GoogleMapInfoWindow w = new GoogleMapInfoWindow(echo.getOwner(), marker);
		this.addMarkerClickListener(new OpenInfoWindowOnMarkerClickListener(this, marker, w));
		this.setCenter(new LatLon(echo.getLat(), echo.getLng()));
		
		this.markersMapByMarker.put(marker,  echo);		
		this.markersMapByShop.put(echo, marker);		
		this.markAsDirty();
	}
	
	public void updateEcho(Echo echo) {
		
		GoogleMapMarker old_marker = this.markersMapByShop.get(echo);
		old_marker.setPosition(new LatLon(echo.getLat(), echo.getLng()));
		this.setCenter(new LatLon(echo.getLat(), echo.getLng()));
//		this.markersMapByShop.remove(shop);
//		this.markersMapByMarker.remove(old_marker);
//		this.removeMarker(old_marker);
//		
//		
//		GoogleMapMarker marker = new GoogleMapMarker(shop.getName(), new LatLon(shop.getLat(), shop.getLng()), true, "VAADIN/themes/haloEarth/img/appicon24r.png");
//		marker.setAnimationEnabled(true);		
//		
//		this.addMarker(marker);
//		final GoogleMapInfoWindow w = new GoogleMapInfoWindow(shop.getName(), marker);
//		this.addMarkerClickListener(new OpenInfoWindowOnMarkerClickListener(this, marker, w));
//		this.setCenter(new LatLon(shop.getLat(), shop.getLng()));
//		
//		this.markersMapByShop.put(shop,  marker);
//		this.markersMapByMarker.put(marker,  shop);
		
		this.markAsDirty();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public void run() {
//		
//		points = (PointsMapService) SpringHelperService.get().getBean("pointsMapService");
//		while (true) {
//		
//			
//			if (points.getPoints() != null) {
//				Map<String, PointF> pointsMap = points.getPoints();
//				for (String imei : pointsMap.keySet()) {
//					log.info(imei);
//					PointF point = pointsMap.get(imei);
//					GoogleMapMarker old_marker = this.markersMapByImei.get(imei);
//					if (old_marker != null) {
//						old_marker.setPosition(new LatLon(point.x, point.y));
//					} else {
//					
//						GoogleMapMarker marker = new GoogleMapMarker(imei, new LatLon(point.x, point.y), false, "VAADIN/themes/haloEarth/img/appicon24.png"); //"VAADIN/themes/haloEarth/img/appicon60.png");
//						marker.setAnimationEnabled(false);		
//						this.addMarker(marker);
//						
//	//					final GoogleMapInfoWindow w = new GoogleMapInfoWindow(shop.getName(), marker);
//	//					this.addMarkerClickListener(new OpenInfoWindowOnMarkerClickListener(this, marker, w));
//	//					this.setCenter(new LatLon(shop.getLat(), shop.getLng()));
//	//					
//						this.markersMapByImei.put(imei, marker);		
//					
//					}
//					this.markAsDirty();
//				}
//			}	
//			
//			try {
//				Thread.sleep(10000);
//			} catch (Exception e) {
//				
//			}
//		}
//	}

}
