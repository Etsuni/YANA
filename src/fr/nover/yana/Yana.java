/**
*Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
*Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter � cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import fr.nover.yana.assistant_installation.Assistant_Installation;
import fr.nover.yana.model.User;
import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ShakeDetector;
import fr.nover.yana.passerelles.ExpandableListAdapter;

public class Yana extends Activity implements TextToSpeech.OnInitListener{
	
	static EditText IPadress; // Affiche et stocke l'adresse IP
	static TextView tts_pref_false; // Affichage pour pr�venir de l'�tat du TTS
	ImageButton btnRec; // Bouton pour lancer l'initialisation
	ImageView ip_adress; // Affichage et actions du bouton � c�t� de ip_adress
	String recrep="", rep=""; // D�clare les variables correspondant aux divers �l�ments de la conversation avec le RPi
    User user; // Pour l'identit� de l'utilisateur
    boolean bienvenue, changelog=false;
    static boolean bienvenue_fait=false;
    Random random = new Random(); // Pour un message al�atoire
		
    private TextToSpeech mTts;// D�clare le TTS
    
    static boolean testTTS = false, AI, commande_actu=false;
	    
    	// A propos du Service (Intent pour le lancer et servstate pour savoir l'�tat du service)
	public static Intent mShakeService,mEventService;
	public static boolean servstate=false, eventstate=false;
	boolean box_TTS;
	
	String token=""; 
	public static String version;
	String version_ex;
	
	SharedPreferences preferences;
	
		// Conversation et liste de commandes
	int n=1;
	boolean update;
	Handler myHandler = new Handler();
	
    ExpandableListView expListView;
	
		// S'il re�oit un signal Broadcast du Service, il r�agit en cons�quence
	private BroadcastReceiver newRecrep = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			String contenu = intent.getStringExtra("contenu");
			conversation(contenu, "envoi");}};
			
	private BroadcastReceiver newRep = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			String contenu = intent.getStringExtra("contenu");
			conversation(contenu, "reponse");}};
	    
	    // Juste une valeur fixe de r�f�rence pour le r�sultat d'Activit�s lanc�es
	protected static final int RESULT_SPEECH = 1;
	protected static final int OPTION = 2;
	protected static final int TTS = 3;
	
	public void onCreate(Bundle savedInstanceState){
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.interface_yana); // D�finit la layout a utiliser
	    
	    LocalBroadcastManager.getInstance(this).registerReceiver(newRecrep, // D�clare les liens Broadcast avec le Service
	 			new IntentFilter("NewRecrep"));
	    LocalBroadcastManager.getInstance(this).registerReceiver(newRep,
				new IntentFilter("NewRep"));

    	IPadress = (EditText)findViewById(R.id.IPadress); // D�clare les �l�ments visibles
    	tts_pref_false = (TextView) findViewById(R.id.tts_pref_false);
    	btnRec = (ImageButton) findViewById(R.id.btnRec);
    	ip_adress = (ImageView) findViewById(R.id.ip_adress);
    	expListView = (ExpandableListView) findViewById(R.id.ExpLV);

		preferences= PreferenceManager.getDefaultSharedPreferences(this);
		
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pInfo.versionName;
			Log.d("Version","Version de l'application : "+version);} 
		catch (NameNotFoundException e) {e.printStackTrace();}
    	
		IPadress.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT); // D�finit l'EditText comme un champ URL
    	
    	getConfig(); // Actualise la configuration
    	if(!commande_actu){commandes_actu();}
    	if(update){commandes_actu();} // Actualise les commandes si la config correspond

    	ip_adress.setOnClickListener(new View.OnClickListener() { // Lance la configuration si on clique sur l'image � c�t� de l'adresse IP
    		@Override
    		public void onClick(View v){
    			String IP_Adress=IPadress.getText().toString();
    			if(IP_Adress.contains("action.php")){
    				IP_Adress = IP_Adress.replace("action.php", "");
        			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+IP_Adress));
        			startActivity(browserIntent);}
    			else{
    				Toast toast= Toast.makeText(getApplicationContext(),
    			    "Votre adresse n'est pas bonne. :(", 4000);  
    				toast.show();}
    			}});
    	
    	btnRec.setOnClickListener(new View.OnClickListener() {	 // S'effectue lors d'un appui sur le bouton Rec
    		@Override
    		public void onClick(View v){
    			initialisation();}});}
	
	public void onStart(){
	    super.onStart();
	    
    	if(AI){    		
    		Intent SetupWizard = new Intent(this, Assistant_Installation.class);
    		startActivityForResult(SetupWizard, OPTION);}
	    
    	else{
		   	if(bienvenue && box_TTS && !bienvenue_fait){
		   		bienvenue_fait=true;
		   		rep = random_String();
		   		mTts = new TextToSpeech(this, this);}
		   	
		   	if(changelog){
				String Changelog="Impossible de charger les changelogs.";
				
				try{Resources res = getResources();
			        InputStream in_s = res.openRawResource(R.raw.changelog);
			        InputStreamReader in_r = new InputStreamReader(in_s, "UTF-8");

			        char[] b = new char[in_s.available()];
			        in_r.read(b);
			        
			        Changelog = new String(b);} 
				catch (Exception e) {Log.d("Changelog",""+e);}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(Changelog)
					   .setTitle("Changelogs")
				       .setCancelable(false)
				       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	    changelog=false;
				        	    Traitement.verif_aux("Changelog_do", getApplicationContext());}
				       });
				AlertDialog alert = builder.create();
				alert.show();
		   	}
    	}
    }
	
	public void onActivityResult(int requestCode, int resultCode, Intent data){ // S'ex�cute lors d'un retour d'activit�
    switch (requestCode) {
		case RESULT_SPEECH: { // D�s que la reconnaissance vocale est termin�e
			if (resultCode == RESULT_OK && null != data) {
				
				ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				recrep = text.get(0); // Enregistre le r�sultat dans RecRep
				
				String Ordre="", URL=""; // D�clare Ordre
				int n = Traitement.comparaison(recrep); // Compare les String pour trouver un ordre
				if(n<0){Ordre=recrep;} // Si la comparaison a �chou�
				else{ // Sinon, la commande la plus proche de l'ordre est attribu�e � Ordre
					Ordre = Traitement.commandes.get(n); 
					URL = Traitement.parameter.get(Ordre).get(0);}
	
				pretraitement(Ordre, URL, n); // Envoie en Pr�traitement
				break;}}
		
		case OPTION: // D�s un retour de la configuration, il la recharge
			getConfig();
			break;
	}}

	public void onInit(int i){ // S'ex�cute d�s la cr�ation du mTts
		try{
			if(mTts.isLanguageAvailable(Locale.FRENCH)!=TextToSpeech.LANG_AVAILABLE && !testTTS){
				new AlertDialog.Builder(this)
		 	    .setTitle("Le TTS n'est pas en Fran�ais.")
		 	    .setMessage("Android d�tecte que votre dispositif de Synth�se Vocale ne dispose pas du Fran�ais dans ses langues. Voulez-vous installer le Fran�ais ? Appuyez sur Non pour continuer quand m�me.")
		 	    .setNegativeButton("Non", new DialogInterface.OnClickListener() {
		 	        public void onClick(DialogInterface dialog, int which) { 
		 	            testTTS=true;}
		 	     })
		 	    .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
		 	        public void onClick(DialogInterface dialog, int which) { 
		 	        	Intent installIntent = new Intent();
			            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
			            startActivity(installIntent);}
		 	     })
		 	    .show();}
			else{
				testTTS=true;
				mTts.setLanguage(Locale.FRENCH);
				
				if(rep.compareTo("")!=0) mTts.speak(rep,TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
			    rep="";} // Au cas o� Rep reste le m�me � la prochaine d�claration du TTS
		}
		catch(Exception e){
			Toast t = Toast.makeText(getApplicationContext(),
    				"Impossible de v�rifier les langues de votre TTS. Yana va tout de m�me essayer de le lancer.",
    				Toast.LENGTH_SHORT);
    	        	t.show();
    	        	if(rep.compareTo("")!=0) mTts.speak(rep,TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
    			    rep="";}
	}

	public void onDestroy(){ // Quitte le TTS quand l'application se termine
	    if (mTts != null){
	        mTts.stop();
	        mTts.shutdown();}
	    super.onDestroy();}
	
	public void onResume(){
		getConfig();
		commandes_Layout();
		super.onResume();}
	
    public boolean onCreateOptionsMenu(Menu menu) { // Il dit juste que y'a telle ou telle chose dans le menu
		getMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);}   
	
	public boolean onOptionsItemSelected(MenuItem item){ // Il dit que si on clique sur tel objet, on effectue telle action
		if(item.getItemId() == R.id.Btnconfiguration){
			startActivityForResult(new Intent(this, Configuration.class), OPTION);}
		if(item.getItemId() == R.id.updateCom){
			commandes_actu();}
		return super.onOptionsItemSelected(item);}

    void getConfig(){ // Importe les param�tres
    	String ip_adress;
    	if(Traitement.verif_Reseau(getApplicationContext())) ip_adress=preferences.getString("IPadress", "");// Importe l'adresse du RPi
    	else ip_adress=preferences.getString("IPadress_ext", "");// Importe l'adresse du RPi
    	
    	if(ip_adress != "") IPadress.setText(ip_adress);
    	
    	AI = preferences.getBoolean("AI", true);
    		
    	box_TTS=preferences.getBoolean("tts_pref", true); // Importe l'�tat de la box (autorise ou non le TTS)
    	if(box_TTS==false) tts_pref_false.setText("Attention ! Votre TTS est d�sactiv�.");
    	else tts_pref_false.setText("");
    	
    	Log.d("","test");
    	
    	bienvenue=preferences.getBoolean("bienvenue", true);
    	
	    version_ex=preferences.getString("version", "");
		if(version.compareTo(version_ex)!=0) changelog=true;
    	
    	update=preferences.getBoolean("update", false);
    	
    	token=preferences.getString("token", "");
    	
    	user = new User();
    	user.nom=preferences.getString("name", ""); // Importe l'identit� de la personne
    	user.prenom=preferences.getString("surname", "");
    	user.sexe=preferences.getString("sexe", "");
    	user.pseudo=preferences.getString("nickname", "");
    	
    	mShakeService=new Intent(Yana.this, ShakeService.class); // D�marre le service en fonction de l'�tat de la box
    	boolean Box_shake=preferences.getBoolean("shake", false);
    	if((Box_shake==true) && servstate==false){startService(mShakeService);}
    	else if((Box_shake==false) && servstate==true){stopService(mShakeService);}
    	else if((Box_shake==true) && servstate==true){ // R�actualise les variables au cas o� on passe d'une reco en continu � une reco par Shake
    		stopService(mShakeService);
    		startService(mShakeService);}
    	
    	mEventService=new Intent(Yana.this, EventService.class); // D�marre le service en fonction de l'�tat de la box
    	boolean Box_Event=preferences.getBoolean("event", false);
    	if((Box_Event==true) && eventstate==false){
    		startService(mEventService);
    		EventService.first=false;}
    	else if((Box_Event==false) && eventstate==true){stopService(mEventService);}
    	
    	Traitement.voice_Sens = Double.parseDouble(preferences.getString("Voice_sens", "3.0"))* Math.pow(10.0,-2.0); // Importe la sensibilit� de la comparaison des chaines de caract�res
    	if (Traitement.voice_Sens>=1){
    		Toast t = Toast.makeText(getApplicationContext(),
    				"Attention ! La sensibilit� d'analyse de la voix est trop forte. Votre programme choisira la commande la plus proche de votre ordre. Pour mettre une sensibilit�, votre valeur dans les options doit �tre inf�rieure � 10. ",
    				Toast.LENGTH_SHORT);
    	        	t.show();}
    	
    	float Shake_sens=Float.parseFloat(preferences.getString("shake_sens", "3.0f")); // Importe la sensibilit� du Shake
		ShakeDetector.getConfig(Shake_sens);

		Log.d("End of Config","End Of Config");}
        
    void initialisation(){ // Initialise le processus
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

    void conversation(String texte, String envoi){ // Ici on inscrit la conversation entre l'utilisateur et le RPi
    	
    	final View conversation_layout =  findViewById(R.id.conversation);
    	
        TextView valueTV = new TextView(this); // Cr�� le TextView pour afficher le message
        valueTV.setText(texte);
        valueTV.setId(n);

        ImageView fleche = new ImageView(this); // Importe la petite fl�che de droite ou de gauche
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params_fleche = new RelativeLayout.LayoutParams(20, 20);
        	
        if(envoi=="envoi"){
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
        ((ViewGroup) conversation_layout).addView(valueTV);
        ((ViewGroup) conversation_layout).addView(fleche);
        
        ((ScrollView) findViewById(R.id.conversation_scroll)).post(new Runnable(){
            public void run(){((ScrollView) findViewById(R.id.conversation_scroll)).fullScroll(View.FOCUS_DOWN);}}); // Pour ancrer en bas � chaque nouvel ordre
    	}
    
	void commandes_Layout(){ // Ici, on va inscrire les commandes sur le panel
		
		if(Traitement.categories.size()>0 && Traitement.listDataChild.size()>0){
			ExpandableListAdapter listAdapter = new ExpandableListAdapter(this, Traitement.categories, Traitement.listDataChild);
	        expListView.setAdapter(listAdapter);
	        
	        expListView.setOnChildClickListener(new OnChildClickListener() {
				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long itemID) {
	    	    	int ID=(int)itemID;
	    	    	ArrayList<String> reco = Traitement.listDataChild.get(Traitement.categories.get(groupPosition));
	    	    	int i = Traitement.comparaison(reco.get(ID));
					pretraitement(Traitement.commandes.get(i), Traitement.parameter.get(Traitement.commandes.get(i)).get(0), i);
	    	    	return false;}
			});
	    }
    	
    	ListView commandes_List =(ListView) findViewById(R.id.commandes_layout);
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, R.drawable.command_list, Traitement.commandes_a);
		commandes_List.setAdapter(modeAdapter);
	        
		commandes_List.setOnItemClickListener(new OnItemClickListener() {
    	    public void onItemClick(AdapterView<?> arg0, View view, int arg2,long itemID) {
    	    	int ID=(int)itemID;
    	    	int i = Traitement.comparaison(Traitement.commandes_a.get(ID));
				pretraitement(Traitement.commandes.get(i), Traitement.parameter.get(Traitement.commandes.get(i)).get(0), i);}
			});}

    void commandes_actu(){ // Ici on va actualiser la liste des commandes
    	ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
 		ArrayList<String> params = new ArrayList<String>();
		
    	if(commande_actu && token.compareTo("")!=0){
    		if(activeNetwork!=null){
		    	if(Traitement.pick_JSON(IPadress.getText().toString(), token)){ // Commence le protocole de reception et les enregistre dans une ArrayList
		    		Toast toast= Toast.makeText(getApplicationContext(), 
		    				"Update fait !", 4000);  
		    				toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
		    				toast.show();}
		    	
		    	else{
		    		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
		    		Traitement.commandes.get(Traitement.commandes.size()-1), 4000);  
					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
					toast.show();
					Traitement.commandes_a = new ArrayList<String>(Traitement.commandes);}}
		    else{
		    	Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
			    	"Vous n'avez pas de connexion internet !", 4000);  
					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
					toast.show();
					Traitement.commandes_a = new ArrayList<String>(Traitement.commandes);}
    	}
    	
    	else if (token.compareTo("")==0 && !AI){ 
    		
    		Traitement.commandes.clear();
    		Traitement.parameter= new HashMap<String, ArrayList<String>>();
    		
    		Traitement.add_Commandes(false);
			
    		String command = "Vous n'avez pas entr� le Token. L'application ne peut pas communiquer avec votre Raspberry Pi.";
    		Traitement.commandes.add(command);
    		params.add("");
    		params.add("");
    		Traitement.parameter.put(command, params);
    		
    		
    		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
    		Traitement.commandes.get(Traitement.commandes.size()-1), 4000);  
    		toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    		toast.show();
    		
    		Traitement.commandes_a = new ArrayList<String>(Traitement.commandes);}
    	
    	else{
    		commande_actu=true;
    		Traitement.commandes.clear();
    		Traitement.parameter = new HashMap<String, ArrayList<String>>();
    		
    		Traitement.add_Commandes(false);
			
    		String Command = "Vous n'avez pas encore actualis� vos commandes.";
    		Traitement.commandes.add(Command);
    		params.add("");
    		params.add("");
    		Traitement.parameter.put(Command, params);

		 	Traitement.commandes_a = new ArrayList<String>(Traitement.commandes);}
    	
    	commandes_Layout();}
    
    void pretraitement(final String ordre, final String URL, final int n){ // Ici, on va analyser la r�ponse si elle est traitable localement. Sinon, on l'envoie au RPi
    	conversation(ordre, "envoi");
    	
    	if(n>0){
			ArrayList<String> params = Traitement.parameter.get(ordre);
			Log.d("","Params : "+params);
	    	
	    	if(params.size()>2){
	        	String reponse="";
	    		String type = params.get(2);
	    		if(type.equals("talk")){
	    			reponse = params.get(3);
		    		if(box_TTS==true && !Traitement.sons){ // Lance la synth�se vocale si les options l'autorisent
						mTts = new TextToSpeech(this, this);}}
	    		else if(type.equals("sound")){
	    			String son = params.get(3);
	    			reponse = "*"+son+"*";
	    			try{int ID = getResources().getIdentifier(son, "raw", "fr.nover.yana");
	    			
		    			MediaPlayer mp = MediaPlayer.create(this, ID); 
		    			mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
		    			mp.start();}
	    			catch(Exception e){}
	    		}
   
	    		if(reponse.compareTo("")==0) conversation(reponse, "reponse");}
	    }
    	
    	myHandler.postDelayed(new Runnable(){

		@Override
		public void run() {
			pretraitement2(ordre,URL, n);
		}}, 250);
    }  
    
    void pretraitement2 (String ordre, String URL, int n){ // Deuxi�me partie du Pr�traitement (MyHandler l'oblige pour afficher l'ordre avant le traitement)
    	rep="";
    	
    	if(Traitement.verif_aux(ordre, this)) rep = Traitement.rep;  // V�rification auxiliaire
    	else if(n==-1) rep="Je ne vois aucun ordre qui ressemble � ce que vous avez dit..."; // Si Ordre=Recrep alors c'est que la reconnaissance par pertinence a �chou�
    	else if(URL.compareTo("")==0) rep=""; // Si l'ordre ne contient aucun URL, �a n'est pas utile de l'envoyer au serveur
    	else{
    		ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
     
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        	
        	if(activeNetwork!=null){ // V�rifie le r�seau
        		Log.d("Ordre",""+ordre);
        			rep = Traitement.HTTP_Contact("http://"+IPadress.getText().toString()+"?"+URL+"&token="+token, getApplicationContext());} // Envoie au RPi et enregistre sa r�ponse
        	else{
        		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
    			    	"Vous n'avez pas de connexion internet !", 4000);  
    					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    					toast.show();}
        }

    	Log.d("","Rep : "+rep);
    	
		if(rep.compareTo("")!=0){ // Si la r�ponse n'est pas valide, �a ne sert � rien de la dire ni de l'�noncer
			conversation(rep, "reponse");
			
			if(box_TTS==true && rep.length()<300 && !Traitement.sons){ // Lance la synth�se vocale si les options l'autorisent et si la r�ponse n'est pas trop longue
				mTts = new TextToSpeech(this, this);}
		}}
    
    public String random_String(){ // Choisit une chaine de caract�res au hasard
		ArrayList<String> list = new ArrayList<String>();
		list.add("Bonjour !");
		
		if(!AI){
			if(user.prenom != ""){
				list.add("Salut "+user.prenom+" !");}
			
			if(user.nom != ""){
				list.add("Sinc�res salutations, ma�tre "+user.nom+".");}
			
			if(user.sexe != ""){
				list.add("Bonjour "+user.sexe+" "+user.nom+". Heureux de vous revoir.");}
			
			if(user.pseudo!= ""){
				list.add("Coucou mon petit "+user.pseudo+". Heureux de te revoir !");}}
		
		int randomInt = random.nextInt(list.size());
        String retour = list.get(randomInt).toString();
		
		return retour;}
    
}