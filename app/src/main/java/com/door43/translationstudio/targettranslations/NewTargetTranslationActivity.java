package com.door43.translationstudio.targettranslations;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.library.Searchable;
import com.door43.translationstudio.util.AppContext;

public class NewTargetTranslationActivity extends AppCompatActivity implements TargetLanguageListFragment.OnItemClickListener, ProjectListFragment.OnItemClickListener, SourceLanguageListFragment.OnItemClickListener {

    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    public static final int RESULT_DUPLICATE = 2;
    private static final String STATE_TARGET_TRANSLATION_ID = "state_target_translation_id";
    private static final String STATE_TARGET_LANGUAGE_ID = "state_target_language_id";
    private TargetLanguage mSelectedTargetLanguage = null;
    private Searchable mFragment;
    private String mNewTargetTranslationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_target_translation);

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                return;
            }

            mFragment = new TargetLanguageListFragment();
            ((TargetLanguageListFragment) mFragment).setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.fragment_container, (TargetLanguageListFragment) mFragment).commit();
            // TODO: animate
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            mNewTargetTranslationId = savedInstanceState.getString(STATE_TARGET_TRANSLATION_ID, null);
            String targetLanguageId = savedInstanceState.getString(STATE_TARGET_LANGUAGE_ID, null);
            if(targetLanguageId != null) {
                mSelectedTargetLanguage = AppContext.getLibrary().getTargetLanguage(targetLanguageId);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_target_translation, menu);
        return true;
    }

    @Override
    public void onItemClick(TargetLanguage targetLanguage) {
        mSelectedTargetLanguage = targetLanguage;

        // display project list
        mFragment = new ProjectListFragment();
        ((ProjectListFragment) mFragment).setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, (ProjectListFragment) mFragment).commit();
        // TODO: animate
        invalidateOptionsMenu();
    }

    @Override
    public void onItemClick(String projectId) {
        Translator translator = AppContext.getTranslator();
        TargetTranslation existingTranslation = translator.getTargetTranslation(mSelectedTargetLanguage.getId(), projectId);
        if(existingTranslation == null) {
            // create new target translation
            TargetTranslation targetTranslation = AppContext.getTranslator().createTargetTranslation(mSelectedTargetLanguage, projectId);
            mNewTargetTranslationId = targetTranslation.getId();

            // display source language list (for first tab)
            mFragment = new SourceLanguageListFragment();
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                extras = new Bundle();
            }
            extras.putString(SourceLanguageListFragment.ARG_PROJECT_ID, projectId);
                    ((SourceLanguageListFragment) mFragment).setArguments(extras);
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, (SourceLanguageListFragment) mFragment).commit();
            // TODO: animate
            invalidateOptionsMenu();
        } else {
            // that translation already exists
            Intent data = new Intent();
            data.putExtra(EXTRA_TARGET_TRANSLATION_ID, existingTranslation.getId());
            setResult(RESULT_DUPLICATE, data);
            finish();
        }
    }

    @Override
    public void onItemClick(SourceLanguage sourceLanguage) {
        // TODO: set tab setting in target translation

        Intent data = new Intent();
        data.putExtra(EXTRA_TARGET_TRANSLATION_ID, mNewTargetTranslationId);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchViewAction = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchViewAction.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mFragment.onSearchQuery(s);
                return true;
            }
        });
        searchViewAction.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_search:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if(mNewTargetTranslationId != null) {
            outState.putSerializable(STATE_TARGET_TRANSLATION_ID, mNewTargetTranslationId);
        } else {
            outState.remove(STATE_TARGET_TRANSLATION_ID);
        }
        if(mSelectedTargetLanguage != null) {
            outState.putString(STATE_TARGET_LANGUAGE_ID, mSelectedTargetLanguage.getId());
        } else {
            outState.remove(STATE_TARGET_LANGUAGE_ID);
        }

        super.onSaveInstanceState(outState);
    }
}
