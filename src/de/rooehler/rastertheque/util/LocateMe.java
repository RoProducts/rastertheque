//package de.rooehler.rastertheque.util;
//
//import java.util.List;
//
//import org.mapsforge.core.model.LatLong;
//import org.mapsforge.map.android.view.MapView;
//import org.mapsforge.map.layer.renderer.TileRendererLayer;
//
//import android.content.Context;
//import android.location.Criteria;
//import android.location.Location;
//import android.location.LocationManager;
//import android.util.Log;
//
//public class LocateMe {
//	
//	
//	/*
//	 * Trying to get the Location <only> for orientation purposes from LocationManager
//	 */
//	public static LatLong locateMeForLatLong(Context context){
//		Location l = locateMeForLocation(context);
//		if(l!=null){
//			return new LatLong(l.getLatitude(), l.getLongitude());
//		}else{
//			return null;
//		}
//	}
//	
//	public static Location locateMeForLocation(Context context) {
//		LocationManager locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//
//		Location actualLocation = null;
//		Criteria criteria = new Criteria();
//		String bestProvider = locManager.getBestProvider(criteria, true);
//		if(bestProvider != null){
//			actualLocation = locManager.getLastKnownLocation(bestProvider);
//		}
//		if(actualLocation == null){
//			List<String> providers = locManager.getProviders(true);
//			Location l = null;
//
//			for (int i=providers.size()-1; i>=0; i--) {
//				l = locManager.getLastKnownLocation(providers.get(i));
//				if (l != null) {
//					break;
//				}
//			}
//		}
//		
//		return actualLocation; //can be null !
//	}
//	
//	public static boolean mapFileContainsPoint(MapView mapView,LatLong p){
//		try{
//			TileRendererLayer trl = (TileRendererLayer) mapView.getLayerManager().getLayers().get(0);
//			return trl.getMapDatabase().getMapFileInfo().boundingBox.contains(p);
//			
//		}catch(Exception e){
//			Log.e(LocateMe.class.getSimpleName(), "error cheching bounding box");
//			return false;
//		}
//	}
//
//}
