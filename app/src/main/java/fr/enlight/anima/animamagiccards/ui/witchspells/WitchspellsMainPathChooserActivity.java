package fr.enlight.anima.animamagiccards.ui.witchspells;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.databinding.library.baseAdapters.BR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.enlight.anima.animamagiccards.R;
import fr.enlight.anima.animamagiccards.async.SaveWitchspellsAsyncTask;
import fr.enlight.anima.animamagiccards.async.SpellbooksLoader;
import fr.enlight.anima.animamagiccards.databinding.ActivityWitchspellsPathChooserBinding;
import fr.enlight.anima.animamagiccards.ui.AnimaBaseActivity;
import fr.enlight.anima.animamagiccards.ui.witchspells.viewmodels.WitchspellsMainPathChooserListener;
import fr.enlight.anima.animamagiccards.ui.witchspells.viewmodels.WitchspellsMainSpellbookViewModel;
import fr.enlight.anima.animamagiccards.ui.witchspells.viewmodels.freeaccess.WitchspellsFreeAccessChooserFragment;
import fr.enlight.anima.animamagiccards.ui.witchspells.viewmodels.secondary.WitchspellsSecondaryPathChooserFragment;
import fr.enlight.anima.animamagiccards.utils.DialogUtils;
import fr.enlight.anima.animamagiccards.views.bindingrecyclerview.BindableViewModel;
import fr.enlight.anima.animamagiccards.views.viewmodels.RecyclerViewModel;
import fr.enlight.anima.cardmodel.business.WitchspellsBusinessService;
import fr.enlight.anima.cardmodel.business.WitchspellsUpdateListener;
import fr.enlight.anima.cardmodel.model.spells.Spellbook;
import fr.enlight.anima.cardmodel.model.spells.SpellbookType;
import fr.enlight.anima.cardmodel.model.witchspells.Witchspells;
import fr.enlight.anima.cardmodel.model.witchspells.WitchspellsPath;
import fr.enlight.anima.cardmodel.utils.SpellUtils;

public class WitchspellsMainPathChooserActivity extends AnimaBaseActivity implements
        WitchspellsMainSpellbookViewModel.Listener,
        WitchspellsSecondaryPathChooserFragment.Listener,
        LoaderManager.LoaderCallbacks<List<Spellbook>>,
        WitchspellsMainPathChooserListener,
        WitchspellsFreeAccessChooserFragment.Listener, WitchspellsUpdateListener {

    public static final String WITCHSPELLS_PATHS_RESULT = "WITCHSPELLS_PARAM";

    private static final String WITCHSPELLS_PARAM = "WITCHSPELLS_PARAM";
    private static final String SECONDARY_DIALOG_FRAGMENT_TAG = "SECONDARY_DIALOG_FRAGMENT_TAG";
    private static final String FREE_ACCESS_DIALOG_FRAGMENT_TAG = "FREE_ACCESS_DIALOG_FRAGMENT_TAG";

    private ActivityWitchspellsPathChooserBinding mBinding;

    private RecyclerViewModel mRecyclerViewModel;

    private List<Spellbook> mSpellbooks;
    private SparseArray<Spellbook> mSpellbookMapping;

    private Witchspells mWitchspells;

    @SuppressLint("UseSparseArrays")
    // Key is the spellbook related id
    private final Map<Integer, WitchspellsPath> mWitchspellsPathMap = new HashMap<>();


    public static Intent navigateForEdition(Context context, Witchspells witchspells) {
        Intent intent = new Intent(context, WitchspellsMainPathChooserActivity.class);
        intent.putExtra(WITCHSPELLS_PARAM, witchspells);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_witchspells_path_chooser);

        mWitchspells = getIntent().getParcelableExtra(WITCHSPELLS_PARAM);

        if(mWitchspells == null) {
            throw new IllegalStateException("Witchspell should not be null at this point");
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        updateTitle();

        for (WitchspellsPath path : mWitchspells.witchPaths) {
            mWitchspellsPathMap.put(path.pathBookId, path);
        }

        mRecyclerViewModel = new RecyclerViewModel();
        mRecyclerViewModel.setLayoutManager(new LinearLayoutManager(this));

        mBinding.setListener(this);

        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_name, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_edit){
            createEditNameDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        WitchspellsBusinessService.addWitchspellsListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        WitchspellsBusinessService.removeWitchspellsListener(this);
    }

    @Override
    public Loader<List<Spellbook>> onCreateLoader(int id, Bundle args) {
        return new SpellbooksLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<Spellbook>> loader, List<Spellbook> spellbooks) {
        mSpellbooks = spellbooks;
        mSpellbookMapping = new SparseArray<>();
        for (Spellbook spellbook : spellbooks) {
            mSpellbookMapping.append(spellbook.bookId, spellbook);
        }

        refreshViewModels();
    }

    @Override
    public void onLoaderReset(Loader<List<Spellbook>> loader) {
        // Nothing to do
    }

    public void refreshViewModels(){
        List<BindableViewModel> result = new ArrayList<>();

        for (Spellbook spellbook : mSpellbooks) {
            SpellbookType bookType = spellbook.spellbookType;

            if(bookType != null && SpellbookType.MAIN_SPELLBOOKS.contains(bookType)){
                WitchspellsPath pathForMainBook = mWitchspellsPathMap.get(spellbook.bookId);

                WitchspellsMainSpellbookViewModel viewModel = new WitchspellsMainSpellbookViewModel(spellbook, pathForMainBook,this);

                result.add(viewModel);
            }
        }

        mRecyclerViewModel.setViewModels(result);
        mBinding.setVariable(BR.model, mRecyclerViewModel);
    }

    // region Callbacks

    @Override
    public void onWitchspellPathUpdated(@NonNull WitchspellsPath witchspellsPath) {
        int pathBookId = witchspellsPath.pathBookId;

        if(witchspellsPath.pathLevel > 0) {
            witchspellsPath.freeAccessSpellsIds = SpellUtils.reevaluateFreeAccessMap(witchspellsPath, null);
            mWitchspellsPathMap.put(pathBookId, witchspellsPath);

        } else if (mWitchspellsPathMap.containsKey(pathBookId)){
            mWitchspellsPathMap.remove(pathBookId);
        }

        updateWitchspellsWithoutRefresh();
    }

    @Override
    public void onShowSecondarySpellbookForMainPath(Spellbook mainSpellbook) {
        ArrayList<Spellbook> spellbooks = SpellUtils.extractSecondarySpellbooks(this, mainSpellbook, mSpellbookMapping);

        WitchspellsSecondaryPathChooserFragment.newInstance(spellbooks, mainSpellbook.bookId)
                .show(getFragmentManager(), SECONDARY_DIALOG_FRAGMENT_TAG);
    }

    @Override
    public void onShowFreeAccessSpells(Spellbook mainSpellbook) {
        WitchspellsPath witchspellsPath = mWitchspellsPathMap.get(mainSpellbook.bookId);
        if(witchspellsPath.freeAccessSpellsIds == null || witchspellsPath.freeAccessSpellsIds.isEmpty()){
            witchspellsPath.freeAccessSpellsIds = SpellUtils.reevaluateFreeAccessMap(witchspellsPath, mainSpellbook.spellbookType);
        }

        WitchspellsFreeAccessChooserFragment.newInstance(witchspellsPath)
                .show(getFragmentManager(), FREE_ACCESS_DIALOG_FRAGMENT_TAG);
    }

    @Override
    public void onDeleteSecondaryPath(final int mainPathId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.Witchspells_Delete_Secondary_Path_Title)
                .setMessage(R.string.Witchspells_Delete_Secondary_Path_Message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        resetSecondaryPath(mainPathId);
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    @Override
    public void onSecondaryPathChosen(int mainPathId, SpellbookType secondaryBookSelectedType) {
        updateSecondaryPath(mainPathId, secondaryBookSelectedType.bookId);
    }

    private void resetSecondaryPath(int mainPathId) {
        updateSecondaryPath(mainPathId, 0);
    }

    private void updateSecondaryPath(int mainPathId, int secondaryPathId){
        WitchspellsPath witchspellsPath = mWitchspellsPathMap.get(mainPathId);
        if(witchspellsPath == null){
            throw new IllegalStateException("Witchspells path should not be nut at this point");
        }

        witchspellsPath.secondaryPathBookId = secondaryPathId;

        // Reevaluate free access spells every time the secondary path changes
        witchspellsPath.freeAccessSpellsIds = SpellUtils.reevaluateFreeAccessMap(witchspellsPath, null);

        updateWitchspells();
    }

    @Override
    public void onDeleteFreeAccess(final int mainPathId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.Witchspells_Delete_Free_Access_Title)
                .setMessage(R.string.Witchspells_Delete_Free_Access_Message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        resetFreeAccess(mainPathId);
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    @Override
    public void onFreeAccessSpellsValidated(int mainPathId, Map<Integer, Integer> freeAccessSpellsIds) {
        updateFreeAccess(mainPathId, freeAccessSpellsIds);
    }

    @SuppressLint("UseSparseArrays")
    private void resetFreeAccess(final int mainPathId) {
        updateFreeAccess(mainPathId, new HashMap<Integer, Integer>());
    }

    private void updateFreeAccess(int mainPathId, Map<Integer, Integer> freeAccessSpellsIds){
        WitchspellsPath witchspellsPath = mWitchspellsPathMap.get(mainPathId);
        witchspellsPath.freeAccessSpellsIds = freeAccessSpellsIds;

        updateWitchspells();
    }

    @Override
    public void onValidationClicked() {
        updateWitchspells();
        finish();
    }

    public void updateWitchspells(){
        mWitchspells.witchPaths = new ArrayList<>(mWitchspellsPathMap.values());

        new SaveWitchspellsAsyncTask().execute(mWitchspells);
    }

    public void updateWitchspellsWithoutRefresh(){
        mWitchspells.witchPaths = new ArrayList<>(mWitchspellsPathMap.values());

        new SaveWitchspellsAsyncTask(false).execute(mWitchspells);
    }

    @Override
    public void onWitchspellsUpdated() {
        refreshViewModels();
    }

    // endregion


    private void createEditNameDialog() {
        ViewGroup view = (ViewGroup) findViewById(R.id.parent_layout);
        DialogUtils.showEditTextDialog(view, R.string.Witchspells_Choose_Witch_Name, R.string.Witchspells_Witch_Name, mWitchspells.witchName, new DialogUtils.EditTextDialogListener() {
            @Override
            public void onTextValidated(DialogInterface dialog, String textValue) {
                if(TextUtils.isEmpty(textValue)){
                    Toast.makeText(WitchspellsMainPathChooserActivity.this, R.string.Error_No_Witchspells_Name, Toast.LENGTH_SHORT).show();
                } else {
                    mWitchspells.witchName = textValue;
                    updateTitle();
                    updateWitchspells();
                }
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void updateTitle() {
        getSupportActionBar().setTitle(getString(R.string.Witchspells_Name_Format, mWitchspells.witchName));
    }
}
