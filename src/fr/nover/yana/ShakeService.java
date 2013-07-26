/**
*Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
*Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter � cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ScreenReceiver;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ShakeDetector.OnShakeListener;

@SuppressLint({ "NewApi", "ShowToast" })
public class ShakeService extends Service implements TextToSpeech.OnInitListener, RecognitionListener{

	private ShakeDetector mShakeDetector; // Pour la d�tection du "shake"
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    private TextToSpeech mTts; // D�clare le TTS
    Random random = new Random(); // Pour un message al�atoire
    
	String A_envoyer, A_dire="", IPadress, Token; // D�clare les deux variables de conversation et l'adresse IP
    
    Boolean last_init=false, last_shake=false; // D�clare les variables pour �viter les doublons d'initialisation et Shake
    
		// Logger tag
 	private static final String TAG="";
 		// D�clare le SpeechRecognizer
 	private static SpeechRecognizer speech = null;
 		// Temps avant arr�t de la reconnaissance
 	private Timer speechTimeout = null;
 	
 		// D�clare les contacts avec l'activit� Yana
 	Intent NewRecrep = new Intent("NewRecrep"); 
 	Intent NewRep = new Intent("NewRep");
 	
 		// Valeur de retour de la Comparaison
 	int n=-1;
     
 		// Timer task used to reproduce the timeout input error that seems not be called on android 4.1.2
	public class SilenceTimer extends TimerTask {
		@Override
		public void run() {
        	Looper.prepare();
        	fin();
    		speechTimeout.cancel();
			onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);}
	}
	
	public void onCreate(){
    	super.onCreate();
    	
    	Yana.ServiceState(true); // D�finit l'�tat du service pour Yana
        
    	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // D�finit tous les attributs du Shake
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override 
            public void onShake(int count) { // En cas de Shake, si l'�cran est allum� et qu'il n'y a pas d�j� eu Shake
            	if(last_shake==false && ScreenReceiver.wasScreenOn){
            		last_shake=true;
            		A_dire=Random_String();
            		getTTS();}
        }});
        
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON); // Pour le contact avec l'interface de Yana
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        final BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        
        fin();}

    public void onDestroy() { // En cas d'arr�t du service
        super.onDestroy();
        Yana.ServiceState(false); // D�finit l'�tat du service (�teint)
        mShakeDetector.setOnShakeListener(new OnShakeListener(){ // Arr�te le Shake
            @Override
            public void onShake(int count) {
            	//Disable 
                }
            });
        if (mTts != null){ // Arr�te de TTS
        	mTts.stop();
            mTts.shutdown();}
	    fin();
    }
    
	public void onInit(int status) { // En cas d'initialisation (apr�s avoir initialis� le TTS)
		SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		if(Traitement.Verif_Reseau(getApplicationContext())){
			IPadress=preferences.getString("IPadress", "");} // Importe l'adresse du RPi
    	else{IPadress=preferences.getString("IPadress_ext", "");}
		
		Token=preferences.getString("token", "");
		
		Toast t = Toast.makeText(getApplicationContext(),A_dire,Toast.LENGTH_SHORT); // Affiche la phrase dites par votre t�l�phone
		t.show();
		
		if(preferences.getBoolean("tts_pref", true)==true){ // Si on autorise le TTS
			mTts.speak(A_dire,TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
		if(last_init==false){ // Si il n'a pas d�j� initialis� le processus de reconnaissance vocale
			android.os.SystemClock.sleep(1000);
			startVoiceRecognitionCycle();} // Il l'effectue au bout d'une seconde
		else{fin();}} // Sinon il remet tout � 0
		}
	
	public void getTTS(){mTts = new TextToSpeech(this, this);} // Initialise le TTS

	public IBinder onBind(Intent arg0) {return null;}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	@SuppressLint("NewApi")
	private SpeechRecognizer getSpeechRevognizer(){ // Configure la reconnaissance vocale
		if (speech == null) {
			speech = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
			speech.setRecognitionListener(this);}
		return speech;}

	@TargetApi(Build.VERSION_CODES.FROYO)
	@SuppressLint("NewApi")
	public void startVoiceRecognitionCycle(){ // D�marre le cycle de reconnaissance vocale
		last_init=true;
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"fr.nover.yana");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
		getSpeechRevognizer().startListening(intent);}

	public void stopVoiceRecognition(){ // Arr�te le cicle de reconnaissance vocale
		speechTimeout.cancel();
		fin();}

	public void onReadyForSpeech(Bundle params) { // D�s que la reconnaissance vocale est pr�te
		Log.d(TAG,"onReadyForSpeech");
		// create and schedule the input speech timeout
		speechTimeout = new Timer();
		speechTimeout.schedule(new SilenceTimer(), 5000);
		}

	public void onBeginningOfSpeech() { // D�s qu'on commence � parler
		Log.d(TAG,"onBeginningOfSpeech");
		// Cancel the timeout because voice is arriving
		speechTimeout.cancel();}

	public void onBufferReceived(byte[] buffer) {}

	public void onEndOfSpeech() {Log.d(TAG,"onEndOfSpeech");} // D�s qu'on arr�te de parler

	public void onError(int error) { // Si erreur, il affiche celle correspondante
		String message;
		switch (error){
			case SpeechRecognizer.ERROR_AUDIO:
				message = "Erreur d'enregistrement audio";
				break;
			case SpeechRecognizer.ERROR_CLIENT:
				message = "Client side error";
				break;
			case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
				message = "Erreur de permission";
				break;
			case SpeechRecognizer.ERROR_NETWORK:
				message = "Erreur de r�seau";
				break;
			case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
				message = "Erreur de r�seau (trop long)";
				break;
			case SpeechRecognizer.ERROR_NO_MATCH:
				message = "Il y a eu une erreur en contactant la reconnaissance vocale. Essayez de quitter l'application totalement ou de red�marrer le service.";
				break;
			case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
				message = "La reconnaissance vocale est d�j� utilis�e";
				break;
			case SpeechRecognizer.ERROR_SERVER:
				message = "Erreur du serveur";
				break;
			case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
				message = "Pas d'ordre donn�";
				break;
			default:
				message = "Erreur inconnue";
				break;}

		fin();
		Log.d(TAG,"onError code:" + error + " message: " + message);
		Toast t = Toast.makeText(getApplicationContext(),
				"Annul� : " + message.toString().toString(),
				Toast.LENGTH_SHORT);
		t.show();}

	public void onResults(Bundle results) { // D�s la r�ception du r�sultat
		String Resultat = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0).toString();
		Log.d(TAG,"Ordre : " + Resultat);
		A_dire="";
		if (speech != null) {
			speech.destroy();
			speech = null;}
		
		String Ordre="", URL="";
		n = Traitement.Comparaison(Resultat); // Compare les deux chaines de caract�res
		Log.d(TAG,"Num�ro sortant : " + n);
		if(n<0){ // Si �chec, Ordre=Resultat
			Ordre=Resultat;
			A_dire="Aucun ordre ne semble �tre identifi� au votre.";} 
		else{ // Sinon...
			Ordre = Traitement.Commandes.get(n); // Ordre prend la valeur de la commande choisie
			if(n==0){ // Si le rep�re est �gal � 0
				SharedPreferences.Editor geted = PreferenceManager.getDefaultSharedPreferences(this).edit();
	    		geted.putBoolean("shake", false); // Il va mettre la box dans les options d�coch�e
	    		geted.commit();
	    		if(Yana.servstate==true){ // Il va arr�ter le service s'il est lanc�.
	    			A_dire="Le ShakeService est maintenant d�sactiv�.";}
	    		else{A_dire="Votre service est d�j� d�sactiv�.";}}}
	 	
		NewRecrep.putExtra("contenu", Ordre); // Envoie l'ordre � l'interface
	 	LocalBroadcastManager.getInstance(this).sendBroadcast(NewRecrep);
	 	
	 	if(n>0){ // Si l'ordre est valable
	    	URL = Traitement.Liens.get(n);
	    	
	    	ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
     
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            
	    	if(activeNetwork!=null){ // V�rifie le r�seau
	    		A_dire = Traitement.HTTP_Contact("http://"+IPadress+"?"+URL+"&token="+Token);} // Envoie au RPi et enregistre sa r�ponse
        	else{
        		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
    			    	"Vous n'avez pas de connexion internet !", 4000);  
    					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    					toast.show();}}
		
		while(A_dire==""){android.os.SystemClock.sleep(1000);} // Attend que A_dire soit bien remplie
		
		NewRep.putExtra("contenu", A_dire); // Envoie de la r�ponse � l'interface
	 	LocalBroadcastManager.getInstance(this).sendBroadcast(NewRep);

		getTTS();} // Dicte la r�ponse

	public void onRmsChanged(float rmsdB) {}

	public void onPartialResults(Bundle arg0) {}
    		
	@SuppressLint("NewApi")
	public String Random_String(){ // Choisit une chaine de caract�res au hasard
		ArrayList<String> list = new ArrayList<String>();
		list.add("Que voulez-vous, ma�tre ?");
		list.add("Comment puis-je vous aider, boss ?");
		list.add("Un truc � dire, ma poule ?!");
		
		int randomInt = random.nextInt(list.size());
        String Retour = list.get(randomInt).toString();
		
		return Retour;}

	public void onEvent(int arg0, Bundle arg1){}

	public void fin(){ // Finalise le processus
		if (speech != null) {
			speech.cancel();
			speech.destroy();
			speech = null;}
		if(n==0){ // Si "Yana, cache-toi"
			android.os.SystemClock.sleep(7000); // Attend bien la fin de l'�nonciation avant de quitter l'appli
			last_shake=last_init=false;
			this.stopSelf();}
		else{last_shake=last_init=false;} // Sinon...
		}
	
}