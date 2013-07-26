/**
*Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
*Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter � cette adresse : Etsu@live.fr
**/

package fr.nover.yana.passerelles;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class Traitement {
	
	static String Reponse=""; // D�clare la variable de r�ponse
	
 	private static final String TAG=""; // Logger tag
 	public static double Voice_Sens; // Sensibilit� de la comparaison entre ordres et commandes
 	static public String URL="";
 	
 		// D�clare les ArraList utilis�es pour stocker les �l�ments de commandes
 	public static ArrayList<String> Commandes = new ArrayList<String>();
 	public static ArrayList<String> Liens = new ArrayList<String>();
 	public static ArrayList<String> Confidences = new ArrayList<String>();
 	
 	static boolean retour; // D�clare le retour pour la passerelles JsonParser
 	
 	static JSONObject json;
	
	public static int Comparaison(String Enregistrement){ // Processus de comparaison pour faire une reconnaissance par pertinence
		int n=-1;
		double c=0,co;
		try{
			for (int i = 0; i < Commandes.size(); i++){
			co=LevenshteinDistance.similarity(Enregistrement, Commandes.get(i));
			if(co>c && co>Double.parseDouble(Confidences.get(i))-Voice_Sens){
				c=co;
				n=i;}
		}}
		catch(Exception e){Log.e("log_tag", "Erreur pour la comparaison : "+e.toString());}
		if(c<Voice_Sens){n=-1;} // Compare en fonction de la sensibilit� (cf option)
		return n;} // Retourne le r�sultat
	
	public static String HTTP_Contact(String URL){
		Log.d(TAG,"Echange avec le serveur");
	    
	    	JsonParser jParser = new JsonParser ();
	    try{json = jParser.getJSONFromUrl(URL);}
	    catch(Exception e){}
	    	
	    try{JSONArray commands = json.getJSONArray("responses");
			JSONObject Rep = commands.getJSONObject(0); // Importe la premi�re valeur
			Reponse=Rep.getString("sentence");
			for (int i = 1; i < commands.length(); i++) { // Importe les autres valeurs s'il y en a
				JSONObject emp = commands.getJSONObject(i);
				if("talk".compareTo(emp.getString("type"))==0){
					Reponse=Reponse+" \n"+emp.getString("sentence");}}}
		catch(JSONException e){e.printStackTrace();}
		catch(Exception e){
			try{
				JSONArray commands = json.getJSONArray("error");
				JSONObject Rep = commands.getJSONObject(0); // Importe la premi�re valeur
				Reponse=Rep.getString("error");}
			catch(JSONException x){e.printStackTrace();}
			catch(Exception x){
				Reponse="Il y a eu une erreur lors du contact avec le Raspberry Pi.";}
			}
	    	
    	return Reponse;}
	
	@SuppressLint("DefaultLocale")
	public static boolean pick_JSON(String IPadress, String Token){
		retour=true;

		Commandes.clear();
		Liens.clear();
		Confidences.clear();

	 	JsonParser jParser = new JsonParser ();
	 	try{json = jParser.getJSONFromUrl("http://"+IPadress+"?action=GET_SPEECH_COMMAND&token="+Token);}
	 	catch(Exception e){}
	 	
	 	if(retour){
	 		Log.d("","D�but du traitement du JSON");
		 	try{JSONArray commands = json.getJSONArray("commands");
				for(int i = 0; i < commands.length(); i++) {
					JSONObject emp = commands.getJSONObject(i);
					String Command=emp.getString("command");
					if(Command.contains("&#039;")){
						Command = Command.replace("&#039;", "'");}
					String URL = emp.getString("url");
					StringTokenizer tokens = new StringTokenizer(URL, "?");
					tokens.nextToken();
					URL = tokens.nextToken();
					if(!URL.contains("sound")){
						Commandes.add(Command);
						Liens.add(URL);
						Confidences.add(emp.getString("confidence"));}
					}
			}
		
		 	catch(JSONException e){
				Verification_erreur();
				retour=false;}
		 	
			catch(Exception e){
				Verification_erreur();
				retour=false;}}
	 	else{
	 		Liens.add(1, "");
			Confidences.add(1, "");
	 		Commandes.add(1, "Echec du contact avec le serveur. Veuillez v�rifier votre syst�me et l'adresse entr�e.");}
	 	
		Commandes.add(0, "YANA, cache-toi.");
		Liens.add(0, "");
		Confidences.add(0, "0.7");
		
		return retour;}
	
	static void Verification_erreur(){
		Log.d(TAG, "Regarde s'il n'y a pas une erreur disponible.");
		
		Liens.add("");
		Confidences.add("");
				
		try{
			if(json.getString("error").compareTo("insufficient permissions")==0 || json==null){
				Commandes.add("Il y a une erreur d'identification ou vous n'avez pas les permissions n�c�ssaires. V�rifiez votre Token.");}
		}
		catch(JSONException x){
			Log.d(TAG, "Echec de toute compr�hension.");
			Commandes.add("Echec de compr�hension par rapport � la r�ponse du Raspberry Pi. Veuillez pr�senter le probl�me � Nover.");}

		catch(Exception x){
			Log.d(TAG, "Echec de toute compr�hension.");
			Commandes.add("Echec de compr�hension par rapport � la r�ponse du Raspberry Pi. Veuillez pr�senter le probl�me � Nover.");}
	}

	public static boolean Verif_Reseau(Context context){ // V�rifie le r�seau local
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		   WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		   String SSID_wifi=wifiInfo.getSSID();
		   if(SSID_wifi!=null && SSID_wifi.contains("\"")){
			   SSID_wifi = SSID_wifi.replaceAll("\"", "");}
		   Log.d("SSID",SSID_wifi);
		   SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(context);
		   String SSID_local=preferences.getString("SSID", "");
		   Log.d("SSID demand�e",SSID_local);
		   if(SSID_wifi.compareTo(SSID_local)==0 || SSID_local.compareTo("")==0){
			   Log.d("Local","true");
			   return true;}
		   else{
			   Log.d("Local","false");
			   return false;}
	}
	
}