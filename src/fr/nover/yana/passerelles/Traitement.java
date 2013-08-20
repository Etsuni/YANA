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
	
	static String Reponse=""; // D�clare la variable de r�ponse
	
 	private static final String TAG=""; // Logger tag
 	public static double Voice_Sens; // Sensibilit� de la comparaison entre ordres et commandes
 	static public String URL="", Rep;
 	static public boolean Sons;
 	
 		// D�clare les ArraList utilis�es pour stocker les �l�ments de commandes
 	public static ArrayList<String> Commandes = new ArrayList<String>();
 	public static ArrayList<String> Liens = new ArrayList<String>();
 	public static ArrayList<String> Confidences = new ArrayList<String>();
 	
 	public static ArrayList<String> Categories = new ArrayList<String>();
    public static ArrayList<String> Identifiant_cat = new ArrayList<String>();
 	
    public static HashMap<String, ArrayList<String>> listDataChild;
    public static ArrayList<String> Commandes_a = new ArrayList<String>();
 	
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
		Log.d("Avant Check_VoiceSens", "c="+Voice_Sens);
		Log.d("Avant Check_VoiceSens", "n="+n);
		if(c<Voice_Sens){n=-1;} // Compare en fonction de la sensibilit� (cf option)
		return n;} // Retourne le r�sultat
	
	public static String HTTP_Contact(String URL, Context context){
		Log.d("Echange avec le serveur",""+URL);
		
		try{json = new JsonParser().execute(URL).get();}
		catch(Exception e){Log.d("Echec du contact","Le server n'a pas pu �tre contact�");}
		    	
		try{JSONArray commands = json.getJSONArray("responses");
			JSONObject Rep = commands.getJSONObject(0); // Importe la premi�re valeur
			String type = Rep.getString("type");
				
			if(type.compareTo("talk")==0){
				Reponse=Rep.getString("sentence");}
			
			else if (Rep.getString("type").compareTo("sound")==0){
				String Son = Rep.getString("file");
				Son = Son.replace(".wav","");
				Reponse="*"+Son+"*";
				Media_Player(Son, context);}
				
			for (int i = 1; i < commands.length(); i++) { // Importe les autres valeurs s'il y en a
				JSONObject emp = commands.getJSONObject(i);
				if("talk".compareTo(emp.getString("type"))==0){
					Reponse=Reponse+" \n"+emp.getString("sentence");}
				else if (Rep.getString("type").compareTo("sound")==0){
					String Son = emp.getString("file");
					Son = Son.replace(".wav","");
					Reponse=Reponse+" \n"+"*"+Son+"*";
					Media_Player(Son, context);}	
				}
			}
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

		Reponse = Reponse.replace("&#039;", "'");
		Reponse = Reponse.replace("eteint", "�teint");
    	return Reponse;}
	
	public static boolean pick_JSON(String IPadress, String Token){
		retour=true;

		Commandes.clear();
		Liens.clear();
		Confidences.clear();
		
		Categories.clear();
		Identifiant_cat.clear();
		
        listDataChild = new HashMap<String, ArrayList<String>>();
	 	Commandes_a.clear();

	 	try{json = new JsonParser().execute("http://"+IPadress+"?action=GET_SPEECH_COMMAND&token="+Token).get();}
	 	catch(Exception e){}
	 	
	 	if(retour){
	 		Log.d("","D�but du traitement du JSON");
		 	try{JSONArray commands = json.getJSONArray("commands");
				for(int i = 0; i < commands.length(); i++) {
					JSONObject emp = commands.getJSONObject(i);
					String Command=emp.getString("command");
					Command = Command.replace("&#039;", "'");
					Command = Command.replace("eteint", "�teint");
					String URL = emp.getString("url");
					URL = URL.replace("{", "%7B");
        			URL = URL.replace("}", "%7D");
					StringTokenizer tokens = new StringTokenizer(URL, "?");
					tokens.nextToken();
					URL = tokens.nextToken();
					
					if(!URL.contains("vocalinfo_devmod")){
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
				retour=false;}

		 	Add_Commandes(true);
		 	
		 	ArrayList<String> Links = new ArrayList<String>(Liens);
		 	Commandes_a = new ArrayList<String>(Commandes);

 			 
		 	for (int y=Categories.size()-1; y>=0; y--){
		 		ArrayList<String> Reco = new ArrayList<String>();
		 		for(int i=Commandes_a.size()-1; i>=0; i--){
					if(Links.get(i).toLowerCase().contains(Identifiant_cat.get(y).toLowerCase()) || Commandes_a.get(i).toLowerCase().contains(Identifiant_cat.get(y).toLowerCase())){
						Reco.add(Commandes_a.get(i));
						Commandes_a.remove(i);
						Links.remove(i);}
				}
	 			Log.d("Reco","Reco : "+Reco);
		 		if(Reco.size()>0){
		 			Collections.reverse(Reco); 
		 			listDataChild.put(Categories.get(y), Reco);}
		 		else{ 
		 			Log.d("Remove","Remove : "+Categories.get(y));
		 			Categories.remove(y);}
		 	}
	 	}
	 	else{
	 		Liens.add("");
			Confidences.add("");
	 		Commandes.add("Echec du contact avec le serveur. Veuillez v�rifier votre syst�me et l'adresse entr�e.");}
		
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
	
	public static boolean Verif_aux(String Commande, Context context){
		SharedPreferences.Editor geted = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		if(Commande.contains("cache-toi")){
			geted.putBoolean("shake", false);
			geted.commit();
			if(Yana.servstate==true){
				context.stopService(Yana.mShakeService);
				Rep="Le ShakeService est maintenant d�sactiv�.";}
			else{Rep="Votre service est d�j� d�sactiv�.";}
			return true;}
		
		else if(Commande.contains("montre-toi")){
			geted.putBoolean("shake", true);
			geted.commit();
			if(Yana.servstate==false){
				context.startService(Yana.mShakeService);
				Rep="Le ShakeService est maintenant activ�.";}
			else{Rep="Votre service est d�j� activ�.";}
			return true;}
		
		else if(Commande.contains("d�sactive les �v�nements")){
			geted.putBoolean("event", false);
			geted.commit();
			if(Yana.eventstate==true){
				context.stopService(Yana.mEventService);
				Rep="Les �v�nements sont maintenant d�sactiv�s.";}
			else{Rep="Les �v�nements sont d�j� d�sactiv�s.";}
			return true;}
		
		else if(Commande.contains("active les �v�nements")){
			geted.putBoolean("event", true);
			geted.commit();
			if(Yana.eventstate==false){
				context.startService(Yana.mEventService);
				Rep="Les �v�nements sont maintenant activ�s.";}
			else{Rep="Les �v�nements sont d�j� activ�s.";}
			return true;}
		
		return false;
	}
	
	public static void Add_Commandes(boolean Full){
		int i=0;
		
		Commandes.add(i, "YANA, montre-toi.");
		Liens.add(i, "");
		Confidences.add(i, "0.7");
		i++;
		
		Commandes.add(i, "YANA, cache-toi.");
		Liens.add(i, "");
		Confidences.add(i, "0.7");
		i++;
		
		Commandes.add(i, "YANA, active les �v�nements.");
		Liens.add(i, "");
		Confidences.add(i, "0.7");
		i++;
		
		Commandes.add(i, "YANA, d�sactive les �v�nements.");
		Liens.add(i, "");
		Confidences.add(i, "0.7");
		i++;
		
		if(Full){
			Categories.add("XBMC");
			Identifiant_cat.add("XBMC");
			
			Categories.add("Sons");
			Identifiant_cat.add("vocalinfo_sound");
			
			Categories.add("Relais radio");
			Identifiant_cat.add("radioRelay_change_state");}
		
		Categories.add("Ceci est ajout� uniquement pour �viter de faire des erreurs (car l'ArrayList serait vide)");
		Identifiant_cat.add("Ceci est ajout� uniquement pour �viter de faire des erreurs (car l'ArrayList serait vide)");}
	
	public static void Media_Player(String Son, Context context){
		Sons = true;
		int ID = context.getResources().getIdentifier(Son, "raw", "fr.nover.yana");
		
		MediaPlayer mp = MediaPlayer.create(context, ID); 
		mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		mp.start();}
	
}