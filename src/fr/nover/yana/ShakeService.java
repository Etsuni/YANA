/**
*Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
*Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter � cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import fr.nover.yana.passerelles.SpeechRecognizerWrapper.RecognizerFinishedCallback;
import fr.nover.yana.passerelles.SpeechRecognizerWrapper.RecognizerState;
import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ScreenReceiver;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ShakeDetector.OnShakeListener;
import fr.nover.yana.passerelles.SpeechRecognizerWrapper;

public class ShakeService extends Service implements TextToSpeech.OnInitListener, OnUtteranceCompletedListener, RecognizerFinishedCallback{

	private ShakeDetector mShakeDetector; // Pour la d�tection du "shake"
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SpeechRecognizerWrapper mSpeechRecognizerWrapper;
    
    private TextToSpeech mTts; // D�clare le TTS
    boolean TTS_Box; // Permet de savoir si le TTS est autoris�
	boolean Speech_continu;
    Random random = new Random(); // Pour un message al�atoire
    String Nom, Pr�nom, Sexe, Pseudo; // Pour l'identit� de l'utilisateur
    
	String A_envoyer, A_dire="", IPadress, Token; // D�clare les deux variables de conversation et l'adresse IP
    
    Boolean last_init=false, last_shake=false; // D�clare les variables pour �viter les doublons d'initialisation et Shake
    
    Intent STT;
	Handler myHandler = new Handler();
	Context context;
    
		// Logger tag
 	private static final String TAG="";
 		// D�clare le SpeechRecognizer
 	private static SpeechRecognizer speech = null;
 	
 		// D�clare les contacts avec l'activit� Yana
 	Intent NewRecrep = new Intent("NewRecrep"); 
 	Intent NewRep = new Intent("NewRep");
 	Intent Post_speech = new Intent("Post_speech");
 	
 		// Valeur de retour de la Comparaison
 	int n=-1;
 	
 	final BroadcastReceiver mReceiver = new ScreenReceiver();
	
	public void onCreate(){
    	super.onCreate();
    	
    	Yana.servstate=true; // D�finit l'�tat du service pour Yana
        
    	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // D�finit tous les attributs du Shake
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override 
            public void onShake(int count) { // En cas de Shake, si l'�cran est allum� et qu'il n'y a pas d�j� eu Shake
            	if(last_shake==false && ScreenReceiver.wasScreenOn){
            		last_shake=true;
            		getConfig();
            		A_dire=Random_String();
            		getTTS();}
        }});
        
        mSpeechRecognizerWrapper = new SpeechRecognizerWrapper(getApplicationContext());
        mSpeechRecognizerWrapper.addRecognizerFinishedCallback(this);
        
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON); // Pour le contact avec l'interface de Yana
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
        
        context=this;
        mSpeechRecognizerWrapper.Broad(this);
        
        fin();

        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		Speech_continu=preferences.getBoolean("continu", false);
		Log.d("Speech_continu","Speech_continu : "+Speech_continu);}

    public void onDestroy() { // En cas d'arr�t du service
        super.onDestroy();
        Yana.servstate=false; // D�finit l'�tat du service (�teint)
        unregisterReceiver(mReceiver);
        mShakeDetector.setOnShakeListener(new OnShakeListener(){ // Arr�te le Shake
            @Override
            public void onShake(int count) {
            	//Disable 
                }
            });
        if (mTts != null){ // Arr�te de TTS
        	mTts.stop();
            mTts.shutdown();}
	    fin();}
    
	public void onInit(int status) { // En cas d'initialisation (apr�s avoir initialis� le TTS)
		
		Toast t = Toast.makeText(getApplicationContext(),A_dire,Toast.LENGTH_SHORT); // Affiche la phrase dites par votre t�l�phone
		t.show();
		
		if(TTS_Box){ // Si on autorise le TTS
			mTts.setOnUtteranceCompletedListener(this);
			HashMap<String, String> myHashAlarm = new HashMap<String, String>();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Enonciation termin�e");
			mTts.speak(A_dire,TextToSpeech.QUEUE_FLUSH, myHashAlarm);} // Il dicte sa phrase
		}

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		if(!last_init || (SpeechRecognizerWrapper.mIsListening && Speech_continu)){ // Si il n'a pas d�j� initialis� le processus de reconnaissance vocale
			myHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					LocalBroadcastManager.getInstance(context).sendBroadcast(Post_speech);
				}}, 250);
			}
		else{fin();} // Sinon il remet tout � 0
	}

	public void getTTS(){mTts = new TextToSpeech(this, this);} // Initialise le TTS

	public IBinder onBind(Intent arg0) {return null;}
    		
	public String Random_String(){ // Choisit une chaine de caract�res au hasard
		ArrayList<String> list = new ArrayList<String>();
		list.add("Comment puis-je vous aider, boss ?");
		list.add("Un truc � dire, ma poule ?!");
		
		if(Pr�nom.compareTo("")!=0){
			list.add("Oui, "+Pr�nom+" ?");}
		
		if(Nom.compareTo("")!=0){
			list.add("Que voulez-vous, ma�tre "+Nom+" ?");}
		
		if(Sexe.compareTo("")!=0){
			list.add("Puis-je faire quelque chose pour vous, "+ Sexe+" ?");}
		
		if(Nom.compareTo("")!=0 && Sexe.compareTo("")!=0){
			list.add(Sexe+" "+Nom+", je suis tout � vous !");}
		
		if(Pseudo.compareTo("")!=0){
			list.add("Que veux-tu, mon petit "+Pseudo+" ?");}
		
		int randomInt = random.nextInt(list.size());
        String Retour = list.get(randomInt).toString();
		
		return Retour;}
	
	public void getConfig(){ // Importe les options
		SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		if(Traitement.Verif_Reseau(getApplicationContext())){
			IPadress=preferences.getString("IPadress", "");} // Importe l'adresse du RPi
    	else{IPadress=preferences.getString("IPadress_ext", "");}
		
		Token=preferences.getString("token", "");
		TTS_Box=preferences.getBoolean("tts_pref", true);
		Speech_continu=preferences.getBoolean("continu", true);
		
		Nom=preferences.getString("name", ""); // Importe l'identit� de la personne
		Pr�nom=preferences.getString("surname", "");
		Sexe=preferences.getString("sexe", "");
		Pseudo=preferences.getString("nickname", "");}

	public void onEvent(int arg0, Bundle arg1){}

	public void fin(){ // Finalise le processus
		if (speech != null) {
			speech.cancel();
			speech.destroy();
			speech = null;}

		last_shake=last_init=false;
		if(n==0){ // Si "Yana, cache-toi"
			this.stopSelf();}
		
		else if(Speech_continu){
			last_shake=true;
			last_init=true;}
		}	

    public void onRecognizerFinished(String Resultat) {
        Log.d(TAG,"Ordre : " + Resultat);
        if(!Speech_continu) mSpeechRecognizerWrapper.Stop();last_init=true;
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
			Ordre = Traitement.Commandes.get(n);} // Ordre prend la valeur de la commande choisie
	 	
		NewRecrep.putExtra("contenu", Ordre); // Envoie l'ordre � l'interface
	 	LocalBroadcastManager.getInstance(this).sendBroadcast(NewRecrep);
	 	
	 	if(n>0){ // Si l'ordre est valable
	 		if(Traitement.Verif_aux(Ordre,context)) A_dire=Traitement.Rep;
	 		else{
		    	URL = Traitement.Liens.get(n);
		    	
		    	ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
	                    .getSystemService(Context.CONNECTIVITY_SERVICE);
	     
	            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
	            
		    	if(activeNetwork!=null){ // V�rifie le r�seau
		    		A_dire = Traitement.HTTP_Contact("http://"+IPadress+"?"+URL+"&token="+Token, getApplicationContext());} // Envoie au RPi et enregistre sa r�ponse
	        	else{
	        		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
	    			    	"Vous n'avez pas de connexion internet !", 4000);  
	    					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
	    					toast.show();}}}
		
		if(A_dire.compareTo("")!=0){
			NewRep.putExtra("contenu", A_dire); // Envoie de la r�ponse � l'interface
		 	LocalBroadcastManager.getInstance(this).sendBroadcast(NewRep);}
		else{A_dire=" ";}
		if(!Traitement.Sons) getTTS();
		else Traitement.Sons=false;}
    																													
    @Override
    public void onRecognizerStateChanged(RecognizerState state) {
        Log.d(TAG, state.toString());
        
        if (state == RecognizerState.Error) {
            Toast.makeText(this, state.toString(), Toast.LENGTH_SHORT).show();
            fin();}
    }

    public void onRmsChanged(float rmsdB) {;}

}