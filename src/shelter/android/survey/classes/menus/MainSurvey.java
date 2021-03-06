package shelter.android.survey.classes.menus;

import shelter.android.survey.classes.forms.*;
import shelter.android.survey.classes.widgets.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import shelter.android.survey.classes.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;


/** MainSurvey 20/09/2013 - David Stark
 * 
 * MainSurvey is the class which renders the survey. 
 * It uses the SurveyFormActivity class to generate
 * the Layout for the survey and attaches the sideBar 
 * view, the listener for the back button and the 
 * initiation of the menu with the Save, SaveasDraft, 
 * Load and Cancel buttons. This class requires an Intent 
 * with details of the SlumId, Slum name, subSections, 
 * householdId, survey and section Name.
 * 
 */

public class MainSurvey extends SurveyFormActivity
{
	public static final int OPTION_SAVE = 0;
	public static final int OPTION_LOAD = 1;
	public static final int OPTION_CANCEL = 2;
	public static final int OPTION_SAVEDRAFT = 4;
	private ListView mDrawerList;
	FrameLayout mContentFrame;
	private ArrayList<String> subSections = new ArrayList<String>();
	JSONObject obj = new JSONObject();
	ArrayList<String> sections = new ArrayList<String>();
	String slum;
	String key;
	String survey;
	String slumName;
	String section;
	String householdId;
	JSONObject json_survey;

	PrintWriter writer;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		// Parse intent
		slum = (String)getIntent().getExtras().get("slum");
		slumName = (String)getIntent().getExtras().get("slumName");
		subSections = getIntent().getExtras().getStringArrayList("subSections");
		householdId = (String)getIntent().getExtras().get("householdId");
		survey = (String)getIntent().getExtras().get("surveyId");
		section = (String) getIntent().getExtras().get("section");
		String json_data = parseDownloadToString( "all_surveys.json" );
		key = init(slum, survey, householdId);
		//Setting the title to display in the form SlumName - SectionName
		setTitle(slumName + " - " + section);
		// Render The main survey area
		setContentView(R.layout.main);

		
		/*
		 * If section is passed as an empty String, it's the 
		 * first time the survey has been opened. We then need
		 * to read the existing subsections from the Database.
		 *
		 */
		

		
		
		
		try {
			obj = new JSONObject( json_data );
			json_survey = obj.getJSONObject(survey);
			if(section.equals(""))
			{
				subSections = getSubSections();
			}
			String[] params = {json_survey.toString(), slum, slumName, section, key, survey, householdId} ;
			LinearLayout layout = generateForm(params, db, subSections);
			mContentFrame = (FrameLayout)findViewById(R.id.content_frame);
			mContentFrame.addView(layout);
			// Render the sidebar
			mDrawerList = (ListView)findViewById(R.id.left_drawer);
			sections = getSections(json_survey.toString(), subSections);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, sections);
			mDrawerList.setAdapter(adapter);
			mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
			mDrawerList.setDividerHeight(15);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		// Load existing facts for the current survey
		DatabaseHandler db = new DatabaseHandler(this);
		populate(db, key);
	}
	
	/*
	 * DrawerItemClickListener is the class which is called when 
	 * any of the items on the Side Bar are selected by the user.
	 * The listener calls the selectItem method, which takes the index
	 * of the item selected, and a new intent uses that index to generate
	 * a new form with the section at the selected index.
	 *
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			saveAsDraft(db, key);
			selectItem(position);
			
		}
	}


	private void selectItem(int position)
	{
		section = sections.get(position);
		Intent intent = new Intent(this, MainSurvey.class);
		intent.putExtra("slum", slum);
		intent.putExtra("slumName", slumName);
		intent.putExtra("surveyId", survey);
		intent.putExtra("section", section);
		intent.putExtra("subSections", subSections);
		intent.putExtra("householdId",  householdId);
		mDrawerList.setItemChecked(position, true);
	    startActivity(intent);
	}
	
	/*
	 * init(String slum, String survey, String householdId) checks for 
	 * the existence of a survey entry in the database by using the parameters
	 * as a compound key. Helper methods in the DatabaseHandler class are used, 
	 * and if there is a survey entry, it returns the key for the table, else it
	 * creates the survey entry and returns the key of the new entry.
	 */
	
	public String init(String slum, String survey, String householdId)
	{
		if (db.isInSurvey(slum, survey, householdId) == null)
		{
			db.addSurvey(slum, survey, householdId);
			return db.isInSurvey(slum, survey, householdId);
		}

		return db.isInSurvey(slum, survey, householdId);
	}
	
	/*
	 * getSubSections() uses the key in the current instance to 
	 * lookup the subSections that are in the Database for this 
	 * instance. Returns an ArrayList of the names of the sections
	 * to which the subSections belong.
	 * 
	 */
	
	
	public ArrayList<String> getSubSections()
	{
		ArrayList<String> subSections = new ArrayList<String>();
		Map<Integer, Integer> dbSubSections = db.getSubSections(key);
		for (Map.Entry<Integer, Integer> entry : dbSubSections.entrySet())
		{
		try
		{
		JSONObject schema = obj.getJSONObject(survey);
		JSONArray sections = schema.getJSONArray("sections");
			for(int i = 0; i<sections.length();i++)
			{
				JSONObject section = sections.getJSONObject(i);
				if(section.has("sub_section")){
				JSONArray questions = section.getJSONArray("sub_section");
					for(int j=0;j<questions.length();j++)
					{
						JSONObject question = questions.getJSONObject(j);
						if(question.getString("id").equals(String.valueOf(entry.getValue())))
						{
							subSections.add(section.getString("name"));
						}
					}
				}
			}
		
		}
		catch(JSONException e)
		{
			e.toString();
		}
		}
		
		
		return subSections;
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) 
	{
		menu.add( 0, OPTION_SAVE, 0, "Save" );
		menu.add( 0, OPTION_SAVEDRAFT, 0, "Save as Draft");
		menu.add( 0, OPTION_LOAD, 0, "Load" );
		menu.add( 0, OPTION_CANCEL, 0, "Cancel" );
		return true;
	}

	@Override
	public boolean onMenuItemSelected( int id, MenuItem item )
	{

		switch( item.getItemId() )
		{
		case OPTION_SAVE:
			Boolean saved = false;
			saved = save(db, key, json_survey.toString());
			if (saved)
			{
				Intent intent = new Intent(this, Index.class);
				startActivity(intent);
			}
			break;

		case OPTION_LOAD:
			populate(db, key);
			break;

		case OPTION_SAVEDRAFT:
			saveAsDraft(db, key);
			Toast.makeText(getApplicationContext(), "Draft Save Success!" , Toast.LENGTH_SHORT).show();
			break;

		case OPTION_CANCEL:
			break;
		}

		return super.onMenuItemSelected( id, item );
	}

	@Override
	public void onBackPressed()
	{
		new AlertDialog.Builder(this).setTitle("Do you want to save?")
		.setMessage("Do you wish to save a draft?").setNegativeButton("No", new OnClickListener()
		{
			public void onClick(DialogInterface arg0, int arg1)
			{
				Intent intent  = new Intent(getBaseContext(), Index.class);
				startActivity(intent);
			}
		})
		.setPositiveButton("Yes", new OnClickListener()
		{
			public void onClick(DialogInterface arg0, int arg1)
			{
				saveAsDraft(db, key);
				Intent intent  = new Intent(getBaseContext(), Index.class);
				startActivity(intent);

			}

		}
				).setNeutralButton("Cancel", null).create().show();
	}
	
	public String parseDownloadToString( String filename )
	{
		try
		{
			FileInputStream stream = openFileInput(filename);
			return parseFileToString(stream);

		} catch ( IOException e ) {
			Log.i("Log", "IOException: " + e.getMessage() );
		}
		return null;
	}
}