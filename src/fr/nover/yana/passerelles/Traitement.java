/**
*Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
*Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter � cette adresse : Etsu@live.fr
**/

package fr.nover.yana.passerelles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.nover.yana.EventService;
import fr.nover.yana.Yana;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("DefaultLocale")
public class Traitement {
	
	static String reponse=""; // D�clare la variable de r�ponse
	
 	private static final String TAG=""; // Logger tag
 	public static double voice_Sens; // Sensibilit� de la comparaison entre ordres et commandes
 	public static String URL="", rep;
 	public static boolean sons;
 	
 	
 	
 		// D�clare les ArraList utilis�es pour stocker les �l�ments de commandes
 	public static ArrayList<String> commandes = new ArrayList<String>();
 	public static HashMap<String, ArrayList<String>> parameter;
 	
 	public static ArrayList<String> categories = new ArrayList<String>();
    public static ArrayList<String> identifiant_cat = new ArrayList<String>();
 	
    public static HashMap<String, ArrayList<String>> listDataChild;
    public static ArrayList<String> commandes_a = new ArrayList<String>();
 	
 	static boolean retour; // D�clare le retour pour la passerelles JsonParser
 	
 	static JSONObject json;

	public static int comparaison(String enregistrement){ // Processus de comparaison pour faire une reconnaissance par pertinence
		int n=-1;
		double c=0,co;
		try{
			for (int i = 0; i < commandes.size(); i++){
				String confidence = parameter.get(commandes.get(i)).get(1);
				if (confidence.equals("")) confidence="0.8";
				co=LevenshteinDistance.similarity(enregistrement, commandes.get(i));
				if(co>c && co>Double.parseDouble(confidence)-voice_Sens){
					c=co;
					n=i;}
		}}
		catch(Exception e){Log.e("log_tag", "Erreur pour la comparaison : "+e.toString());}
		Log.d("Traitement - Comparaison", "c="+voice_Sens);
		Log.d("Traitement - Comparaison", "n="+n);
		if(c<voice_Sens){n=-1;} // Compare en fonction de la sensibilit� (cf option)
		return n;} // Retourne le r�sultat
	
	public static String HTTP_Contact(String URL, Context context){
		Log.d("Echange avec le serveur",""+URL);
		retour=true;
		
		try{json = new JsonParser().execute(URL).get();}
		catch(Exception e){Log.d("Traitement - HTTP_Contact","Le server n'a pas pu �tre contact�");}
		
		if(!retour) return "Il y a eu une erreur lors du contact avec Yana-Serveur.";
		    	
		try{JSONArray commands = json.getJSONArray("responses");
			JSONObject Rep = commands.getJSONObject(0); // Importe la premi�re valeur
			String type = Rep.getString("type");
				
			if(type.compareTo("talk")==0) reponse=Rep.getString("sentence");
			
			else if (Rep.getString("type").compareTo("sound")==0){
				String Son = Rep.getString("file");
				Son = Son.replace(".wav","");
				reponse="*"+Son+"*";
				media_Player(Son, context);}
				
			for (int i = 1; i < commands.length(); i++) { // Importe les autres valeurs s'il y en a
				JSONObject emp = commands.getJSONObject(i);
				if("talk".compareTo(emp.getString("type"))==0){
					reponse=reponse+" \n"+emp.getString("sentence");}
				else if (Rep.getString("type").compareTo("sound")==0){
					String Son = emp.getString("file");
					Son = Son.replace(".wav","");
					reponse=reponse+" \n"+"*"+Son+"*";
					media_Player(Son, context);}	
				}
			}
			catch(JSONException e){e.printStackTrace();}
			catch(Exception e){
				try{
					JSONArray commands = json.getJSONArray("error");
					JSONObject Rep = commands.getJSONObject(0); // Importe la premi�re valeur
					reponse=Rep.getString("error");}
				catch(JSONException x){e.printStackTrace();}
				catch(Exception x){
					reponse="Il y a eu une erreur lors du contact avec le Raspberry Pi.";}
				}

    	return reponse;}
	
	public static boolean pick_JSON(String iPadress, String token){
		retour=true;

		commandes.clear();
        parameter = new HashMap<String, ArrayList<String>>();
		
		categories.clear();
		identifiant_cat.clear();
		
        listDataChild = new HashMap<String, ArrayList<String>>();
	 	commandes_a.clear();

	 	try{json = new JsonParser().execute("http://"+iPadress+"?action=GET_SPEECH_COMMAND&token="+token).get();}
	 	catch(Exception e){}
	 	
	 	ArrayList<String> links = new ArrayList<String>();
	 	
	 	if(retour){
	 		Log.d("","D�but du traitement du JSON");
		 	try{JSONArray commands = json.getJSONArray("commands");
				for(int i = 0; i < commands.length(); i++) {
					JSONObject emp = commands.getJSONObject(i);

			 		ArrayList<String> params = new ArrayList<String>();
					
					String command=emp.getString("command");
					command = command.replace("&#039;", "'");
					command = command.replace("eteint", "�teint");
					
					String URL = emp.getString("url");
					URL = URL.replace("{", "%7B");
        			URL = URL.replace("}", "%7D");
					StringTokenizer tokens = new StringTokenizer(URL, "?");
					tokens.nextToken();
					URL = tokens.nextToken();
					params.add(URL);
					
					params.add(emp.getString("confidence"));
					
					String type="", contenu="";
					try{
						emp.getString("PreAction");
						type=emp.getString("type");
						if(type.compareTo("talk")==0) contenu=emp.getString("sentence");
						else if(type.compareTo("sound")==0){
							contenu = emp.getString("file");
							contenu = contenu.replace(".wav", "");
							contenu = contenu.replace(".mp3", "");}
						params.add(type);
						params.add(contenu);}
					catch(Exception e){}
					
					if(!URL.contains("vocalinfo_devmod")){
						commandes.add(command);
						links.add(URL);
						parameter.put(command,params);}
				}
			}
		
		 	catch(JSONException e){
				verification_erreur();
				retour=false;}
		 	
			catch(Exception e){
				verification_erreur();
				retour=false;}

		 	add_Commandes(true);
		 	
		 	commandes_a = new ArrayList<String>(commandes);
		 	
		 	int x=0;
		 	while(commandes_a.size()!=links.size()){
		 		links.add(x, "");
		 		x++;}
		 	
		 	for (int y=categories.size()-1; y>=0; y--){
		 		ArrayList<String> reco = new ArrayList<String>();
		 		for(int i=commandes_a.size()-1; i>=0; i--){
					if(links.get(i).toLowerCase().contains(identifiant_cat.get(y).toLowerCase()) || commandes_a.get(i).toLowerCase().contains(identifiant_cat.get(y).toLowerCase())){
						reco.add(commandes_a.get(i));
						commandes_a.remove(i);
						links.remove(i);}
				}
	 			Log.d("Reco","Reco : "+reco);
		 		if(reco.size()>0){
		 			Collections.reverse(reco); 
		 			listDataChild.put(categories.get(y), reco);}
		 		else{ 
		 			Log.d("Remove","Remove : "+categories.get(y));
		 			categories.remove(y);}
		 	}
	 	}
	 	else{
		 	ArrayList<String> params = new ArrayList<String>();
		 	String command = "Echec du contact avec le serveur. Veuillez v�rifier votre syst�me et l'adresse entr�e. Il est possible aussi que votre connexion est trop lente.";
	 		commandes.add(command);
	 		params.add(""); // Initialise un URL
	 		params.add(""); // Initialise une confidence
	 		parameter.put(command, params);
	 		
	 		add_Commandes(false);}
		
		return retour;}
	
	static boolean verification_erreur(){
		Log.d(TAG, "Regarde s'il n'y a pas une erreur disponible.");
		
		ArrayList<String> params = new ArrayList<String>();
		String command = null;
 		params.add(""); // Initialise un URL
 		params.add(""); // Initialise une confidence
				
		try{
			if(json.getString("error").compareTo("insufficient permissions")==0 || json==null) command = "Vous n'avez pas les droits n�c�ssaires pour effectuer cette action. Contactez votre administrateur.";
			if(json.getString("error").compareTo("invalid or missing token")==0 || json==null) command = "Votre Token est invalide. Veuillez le v�rifier.";
		}
		catch(JSONException x){}
		catch(Exception x){}
		
	Log.d(TAG, "Echec de toute compr�hension.");
	if(command==null) command="Echec de compr�hension par rapport � la r�ponse du Raspberry Pi. Veuillez pr�senter le probl�me � Nover.";
		
	commandes.add(command);
	parameter.put(command, params);
	return false;}

	public static boolean verif_Reseau(Context context){ // V�rifie le r�seau local
		try{WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		    ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		    android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		    String SSID_wifi=wifiInfo.getSSID();
		    if(SSID_wifi!=null && SSID_wifi.contains("\"")){
			   SSID_wifi = SSID_wifi.replaceAll("\"", "");}
		    Log.d("SSID","SSID actuelle : "+SSID_wifi);
		   
		    SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(context);
		    String SSID_local=preferences.getString("SSID", "");
		    Log.d("SSID demand�e","SSID enregistr� : "+SSID_local);
		    if(wifi.isConnected()){
		 	   if(SSID_wifi.compareTo(SSID_local)==0 || SSID_local.compareTo("")==0){
				   Log.d("Local","true");
				   return true;}}}
		catch(Exception e){
			Toast toast= Toast.makeText(context,
    			"Echec de la v�rification du r�seau. Mise en local par d�faut", Toast.LENGTH_SHORT);  
    			toast.show();}
		return false;
	}
	
	public static boolean verif_aux(String commande, Context context){
		SharedPreferences.Editor geted = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		if(commande.contains("cache-toi")){
			geted.putBoolean("shake", false);
			geted.commit();
			if(Yana.servstate==true){
				context.stopService(Yana.mShakeService);
				rep="Le ShakeService est maintenant d�sactiv�.";}
			else{rep="Votre service est d�j� d�sactiv�.";}
			return true;}
		
		else if(commande.contains("montre-toi")){
			geted.putBoolean("shake", true);
			geted.commit();
			if(Yana.servstate==false){
				context.startService(Yana.mShakeService);
				rep="Le ShakeService est maintenant activ�.";}
			else{rep="Votre service est d�j� activ�.";}
			return true;}
		
		else if(commande.contains("d�sactive les �v�nements")){
			geted.putBoolean("event", false);
			geted.commit();
			if(Yana.eventstate==true){
				context.stopService(Yana.mEventService);
				rep="Les �v�nements sont maintenant d�sactiv�s.";}
			else{rep="Les �v�nements sont d�j� d�sactiv�s.";}
			return true;}
		
		else if(commande.contains("active les �v�nements")){
			geted.putBoolean("event", true);
			geted.commit();
			if(Yana.eventstate==false){
				context.startService(Yana.mEventService);
	    		EventService.first=false;
				rep="Les �v�nements sont maintenant activ�s.";}
			else{rep="Les �v�nements sont d�j� activ�s.";}
			return true;}
		
		else if(commande.contains("Changelog_do")){
			geted.putString("version", Yana.version);
			geted.commit();
			return true;}
		
		return false;}
	
	public static void add_Commandes(boolean full){
		int i=0;
		ArrayList<String> params = new ArrayList<String>();
		params.add("");
		params.add("0.7");
		String command;
		
		command = "YANA, montre-toi.";
		commandes.add(i, command);
		parameter.put(command, params);
		i++;
		
		command = "YANA, cache-toi.";
		commandes.add(i, command);
		parameter.put(command, params);
		i++;
		
		command = "YANA, active les �v�nements.";
		commandes.add(i, command);
		parameter.put(command, params);
		i++;
		
		command = "YANA, d�sactive les �v�nements.";
		commandes.add(i, command);
		parameter.put(command, params);
		i++;
		
		if(full){
			categories.add("XBMC");
			identifiant_cat.add("XBMC");
			
			categories.add("Sons");
			identifiant_cat.add("vocalinfo_sound");
			
			categories.add("Relais radio");
			identifiant_cat.add("radioRelay_change_state");
		
			categories.add("Ceci est ajout� uniquement pour �viter de faire des erreurs (car l'ArrayList serait vide)");
			identifiant_cat.add("Ceci est ajout� uniquement pour �viter de faire des erreurs (car l'ArrayList serait vide)");}
		}
	
	public static void media_Player(String son, Context context){
		sons = true;
		int ID = context.getResources().getIdentifier(son, "raw", "fr.nover.yana");
		
		MediaPlayer mp = MediaPlayer.create(context, ID); 
		mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		mp.start();}
	
}