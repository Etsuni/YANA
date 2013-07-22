/**
*Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
*Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter � cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ShakeDetector;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")
public class Yana extends Activity implements TextToSpeech.OnInitListener{
	
	static EditText IPadress; // Affiche et stocke l'adresse IP
	static TextView tts_pref_false; // Affichage pour pr�venir de l'�tat du TTS
	ImageButton btnRec; // Bouton pour lancer l'initialisation
	ImageView ip_adress; // Affichage et actions du bouton � c�t� de ip_adress
	String Recrep="", Rep=""; // D�clare les variables correspondant aux divers �l�ments de la conversation avec le RPi
		
    private TextToSpeech mTts;// D�clare le TTS
    
    static boolean testTTS = false;
	    
    	// A propos du Service (Intent pour le lancer et servstate pour savoir l'�tat du service)
	private Intent ShakeService;
	static boolean servstate=false;
	boolean Box_TTS;
	String Token="";
	
		// Conversation et liste de commandes
	int n=1;
	int m=999;
	boolean update;
	Handler myHandler = new Handler();
	
		// S'il re�oit un signal Broadcast du Service, il r�agit en cons�quence
	private BroadcastReceiver NewRecrep = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			String contenu = intent.getStringExtra("contenu");
			conversation(contenu, "envoi");}};
			
	private BroadcastReceiver NewRep = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			String contenu = intent.getStringExtra("contenu");
			conversation(contenu, "reponse");}};
	    
	    // Juste une valeur fixe de r�f�rence pour le r�sultat d'Activit�s lanc�es
	protected static final int RESULT_SPEECH = 1;
	protected static final int OPTION = 2;
	protected static final int TTS = 3;
	
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState){
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.interface_yana); // D�finit la layout a utiliser
	    
	    LocalBroadcastManager.getInstance(this).registerReceiver(NewRecrep, // D�clare les liens Broadcast avec le Service
	 			new IntentFilter("NewRecrep"));
	    LocalBroadcastManager.getInstance(this).registerReceiver(NewRep,
				new IntentFilter("NewRep"));
	    
    	IPadress = (EditText)findViewById(R.id.IPadress); // D�clare les �l�ments visibles
    	tts_pref_false = (TextView) findViewById(R.id.tts_pref_false);
    	btnRec = (ImageButton) findViewById(R.id.btnRec);
    	ip_adress = (ImageView) findViewById(R.id.ip_adress);
    	
    	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); // Emp�che un bug de contact avec le RPi (je ne sais pas pourquoi :))
    	StrictMode.setThreadPolicy(policy);
    	
		IPadress.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT); // D�finit l'EditText comme un champ URL
    	
    	getConfig(); // Actualise la configuration
    	if(m==999){Commandes_actu();}
    	if(update){Commandes_actu();} // Actualise les commandes
    		
    	ip_adress.setOnClickListener(new View.OnClickListener() { // Lance la configuration si on clique sur l'image � c�t� de l'adresse IP
    		@Override
    		public void onClick(View v){
    			startActivityForResult(new Intent(Yana.this, Configuration.class), OPTION);}});
    	
    	btnRec.setOnClickListener(new View.OnClickListener() {	 // S'effectue lors d'un appui sur le bouton Rec
    		@Override
    		public void onClick(View v){
    			Initialisation();}});}
	
	public void onStart(){
	    super.onStart();
		// V�rifie la pr�sence du TTS. S'il n'y en a pas, il propose une installation
    	Intent checkIntent = new Intent();
	    checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
	    startActivityForResult(checkIntent, TTS);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) // S'ex�cute lors d'un retour d'activit�
    {
    switch (requestCode) {
		case RESULT_SPEECH: { // D�s que la reconnaissance vocale est termin�e
			if (resultCode == RESULT_OK && null != data) {
	
				ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				Recrep = text.get(0); // Enregistre le r�sultat dans RecRep
				
				String Ordre="", URL=""; // D�clare Ordre
				int n = Traitement.Comparaison(Recrep); // Compare les String pour trouver un ordre
				if(n<0){Ordre=Recrep;} // Si la comparaison a �chou�
				else{ // Sinon, la commande la plus proche de l'ordre est attribu�e � Ordre
					Ordre = Traitement.Commandes.get(n); 
					URL = Traitement.Liens.get(n);}
	
				Pr�traitement(Ordre, URL); // Envoie en Pr�traitement
				break;}}
		
		case OPTION: {getConfig();} // D�s un retour de la configuration, il la recharge
		case TTS:{
			if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS && testTTS==false) {
				Intent installIntent = new Intent();
	            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	            startActivity(installIntent);
	            
	            Toast toast= Toast.makeText(getApplicationContext(),
	            "Android ne d�tecte aucun dispositif de Synth�se Vocale (TTS). Veuillez installer un programme de Synth�se Vocale sinon l'application ne sera pas enti�rement fonctionnelle.", 4000);  
				toast.show();}
			else{testTTS=true;}}
	}}

	public void onInit(int i){ // S'ex�cute d�s la cr�ation du mTts
	    mTts.speak(Rep,TextToSpeech.QUEUE_FLUSH,null);}

	public void onDestroy(){ // Quitte le TTS quand l'application se termine
	    if (mTts != null){
	        mTts.stop();
	        mTts.shutdown();}
	    super.onDestroy();}
	
    public boolean onCreateOptionsMenu(Menu menu) { // Il dit juste que y'a telle ou telle chose dans le menu
		getMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);}   
	
	public boolean onOptionsItemSelected(MenuItem item){ // Il dit que si on clique sur tel objet, on effectue telle action
		if(item.getItemId() == R.id.Btnconfiguration){
			startActivityForResult(new Intent(this, Configuration.class), OPTION);}
		if(item.getItemId() == R.id.updateCom){
			Commandes_actu();}
		return super.onOptionsItemSelected(item);}

    void getConfig(){ // Importe les param�tres
    	SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
    	
    	String V_string=preferences.getString("IPadress", ""); // Importe l'adresse du RPi
    	if(V_string != ""){
    		IPadress.setText(V_string);}
    		
    	Box_TTS=preferences.getBoolean("tts_pref", true); // Importe l'�tat de la box (autorise ou non le TTS)
    	if(Box_TTS==false){
    		tts_pref_false.setText("Attention ! Votre TTS est d�sactiv�.");}
    	else{
    		tts_pref_false.setText("");}
    	
    	update=preferences.getBoolean("update", false);
    	
    	Token=preferences.getString("token", "");
    		
    	ShakeService=new Intent(Yana.this, ShakeService.class); // D�marre le service en fonction de l'�tat de la box
    	boolean Box_shake=preferences.getBoolean("shake", true);
    	if((Box_shake==true) && servstate==false){
    		startService(ShakeService);}
    		
    	if((Box_shake==false) && servstate==true){
    		stopService(ShakeService);}
    	
    	Traitement.Voice_Sens = Double.parseDouble(preferences.getString("Voice_sens", "3.0"))* Math.pow(10.0,-2.0); // Importe la sensibilit� de la comparaison des chaines de caract�res
    	if (Traitement.Voice_Sens>=1){
    		Toast t = Toast.makeText(getApplicationContext(),
    				"Attention ! La sensibilit� d'analyse de la voix est trop forte. Votre programme choisira la commande la plus proche de votre ordre. Pour mettre une sensibilit�, votre valeur dans les options doit �tre inf�rieure � 10. ",
    				Toast.LENGTH_SHORT);
    	        	t.show();}
    	
    	float Shake_sens=Float.parseFloat(preferences.getString("shake_sens", "3.0f")); // Importe la sensibilit� du Shake
		if(Shake_sens<=1)   { 		
			Toast t = Toast.makeText(getApplicationContext(),
			"Attention ! Votre sensibilit� de Shake est trop basse donc elle a �t� r�hauss�e � 3.",
			Toast.LENGTH_SHORT);
        	t.show();}
		ShakeDetector.getConfig(Shake_sens);}
        
    void Initialisation(){ // Initialise le processus
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "fr-FR");
			
		try {
			startActivityForResult(intent, RESULT_SPEECH);} // Lance l'acquisition vocale
			
		catch (ActivityNotFoundException a) {
			Toast t = Toast.makeText(getApplicationContext(),
					"Oh bah zut alors ! Ton Android n'a pas install� le STT ou ne le supporte pas. Regarde les options (langue et saisie).",
					Toast.LENGTH_SHORT);
			t.show();}
        }  
    
    public static void ServiceState(boolean etat){ // Etat du service
		servstate=etat;}

    void conversation(String Texte, String Envoi){ // Ici on inscrit la conversation entre l'utilisateur et le RPi
    	
    	final View Conversation_layout =  findViewById(R.id.conversation);
    	
        TextView valueTV = new TextView(this); // Cr�� le TextView pour afficher le message
        valueTV.setText(Texte);
        valueTV.setId(n);

        ImageView fleche = new ImageView(this); // Importe la petite fl�che de droite ou de gauche
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params_fleche = new RelativeLayout.LayoutParams(20, 20);
        	
        if(Envoi=="envoi"){
        	fleche.setImageResource(R.drawable.envoi);
        	params_fleche.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        	params_fleche.addRule(RelativeLayout.ALIGN_BOTTOM, n);
	        	
        	params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        	params.addRule(RelativeLayout.BELOW, (n-1));
	        	
        	valueTV.setBackgroundColor(getResources().getColor(R.color.envoi));}
        
        else{
        	fleche.setImageResource(R.drawable.reponse);
        	params_fleche.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        	params_fleche.addRule(RelativeLayout.ALIGN_BOTTOM, n);

        	params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        	params.addRule(RelativeLayout.BELOW, (n-1));
	        	
        	valueTV.setBackgroundColor(getResources().getColor(R.color.recu));}
        
        n=n+1;

        valueTV.setPadding(10, 10, 10, 10);
        params.setMargins(20, 0, 20, 20);
        params_fleche.setMargins(0, 0, 0, 20);
        
        valueTV.setLayoutParams(params);
        fleche.setLayoutParams(params_fleche);
        ((ViewGroup) Conversation_layout).addView(valueTV);
        ((ViewGroup) Conversation_layout).addView(fleche);
        
        ((ScrollView) findViewById(R.id.conversation_scroll)).post(new Runnable(){
            public void run(){((ScrollView) findViewById(R.id.conversation_scroll)).fullScroll(View.FOCUS_DOWN);}}); // Pour ancrer en bas � chaque nouvel ordre
    	}
    
    void Commandes_Layout(String Commande, int i){ // Ici, on va inscrire les commandes sur le panel
    	
    	final View Conversation_layout =  findViewById(R.id.commandes_layout);
    	
    	if(1000+i<=m){ // Si le TextView a d�j� �t� cr��, il change juste le contenu
    		TextView Commande_TV=(TextView) findViewById(1000+i);
    		Commande_TV.setText(Commande);}
    	else{ // Sinon il le cr�� avec les bons param�tres
	        TextView Commande_TV = new TextView(this);
	        Commande_TV.setText(Commande);
	        Commande_TV.setId(1000+i);
	        
	        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT); // Importation des parametres (en dessous du pr�c�dent)
	        	
	        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	        params.addRule(RelativeLayout.BELOW, (999+i));
	
	        Commande_TV.setPadding(10, 10, 10, 10);
	        params.setMargins(20, 0, 20, 20);
	        
	        Commande_TV.setLayoutParams(params);
	        ((ViewGroup) Conversation_layout).addView(Commande_TV);
	        
	        Commande_TV.setOnClickListener(new View.OnClickListener() {
	    		public void onClick(View v){
	        		Pr�traitement(Traitement.Commandes.get(v.getId()-1000), Traitement.Liens.get(v.getId()-1000));}});
	    	m=m+1;}}

    void Commandes_actu(){ // Ici on va actualiser la liste des commandes
    	if(m>999 && Token.compareTo("")!=0){
	    	if(Traitement.pick_JSON(IPadress.getText().toString(), Token)){ // Commence le protocole de reception et les enregistre dans une ArrayList
	    		Toast toast= Toast.makeText(getApplicationContext(), 
	    				"Update fait !", 4000);  
	    				toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
	    				toast.show();}
	    	
	    	else{
	    		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
	    		Traitement.Commandes.get(1), 4000);  
				toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
				toast.show();}
    	}
    	
    	else if (Token.compareTo("")==0){ 
    		
    		Traitement.Commandes.clear();
    		Traitement.Liens.clear();
    		Traitement.Confidences.clear();
    		
    		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
    		"Vous n'avez pas entr� le Token. L'application ne peut pas communiquer avec votre Raspberry Pi.", 4000);  
    		toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    		toast.show();
    		
    		Traitement.Commandes.add(0, "YANA, cache-toi.");
			Traitement.Liens.add(0, "");
			Traitement.Confidences.add(0, "0.7");
			
    		Traitement.Commandes.add("Vous n'avez pas entr� le Token. L'application ne peut pas communiquer avec votre Raspberry Pi.");
    		Traitement.Liens.add("");
    		Traitement.Confidences.add("");}
    	
    	else{
    		Traitement.Commandes.clear();
    		Traitement.Liens.clear();
    		Traitement.Confidences.clear();
    		
    		Traitement.Commandes.add(0, "YANA, cache-toi.");
			Traitement.Liens.add(0, "");
			Traitement.Confidences.add(0, "0.7");
			
    		Traitement.Commandes.add("Vous n'avez pas encore actualis� vos commandes.");
    		Traitement.Liens.add("");
    		Traitement.Confidences.add("");}
    	
    	for(int i=0; i<Traitement.Commandes.size(); i++){Commandes_Layout(Traitement.Commandes.get(i), i);} // Commence � l'inscrire sur la Layout pr�vue � cet effet
		while(m>=Traitement.Commandes.size()+1000){ // Si il y a moins de commande qu'avant l'update, il effacera les affichages en trop
			TextView Commande_TV=(TextView) findViewById(m);
			Commande_TV.setVisibility(View.GONE);
			m--;}
	}
    
    void Pr�traitement(final String Ordre, final String URL){ // Ici, on va analyser la r�ponse si elle est traitable localement. Sinon, on l'envoie au RPi
    	conversation(Ordre, "envoi");
    	
    	myHandler.postDelayed(new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Pr�traitement2(Ordre,URL);
		}}, 250);
    }  
    
    void Pr�traitement2 (String Ordre, String URL){ // Deuxi�me partie du Pr�traitement (MyHandler l'oblige pour afficher l'ordre avant le traitement)
    	Rep="";
    	  
    	if(Ordre.compareTo(Traitement.Commandes.get(0))==0){ // V�rification du "Yana, cache-toi"
    		SharedPreferences.Editor geted = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		geted.putBoolean("shake", false);
    		geted.commit();
    		if(servstate==true){
    			stopService(ShakeService);
    			Rep="Le ShakeService est maintenant d�sactiv�.";}
    		else{Rep="Votre service est d�j� d�sactiv�.";}
    	}
    	
    	else if(Ordre.compareTo(Recrep)==0){Rep="Aucun ordre ne semble �tre identifi� au votre.";} // Si Ordre=Recrep alors c'est que la reconnaissance par pertinence a �chou�
    	else{Rep = Traitement.HTTP_Contact("http://"+IPadress.getText().toString()+"?"+URL+"&token="+Token);} // Envoie au RPi et enregistre sa r�ponse
		
		conversation(Rep, "reponse");
		
		if(Box_TTS==true && Rep.length()<300){
			mTts = new TextToSpeech(this, this);} // Lance la synth�se vocale si les options l'autorisent et si la r�ponse n'est pas trop longue
    }

}