package fr.enlight.anima.animamagiccards.ui.spells;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.loopeer.cardstack.CardStackView;
import com.loopeer.cardstack.CustomCardStackView;

import java.util.ArrayList;
import java.util.List;

import fr.enlight.anima.animamagiccards.R;
import fr.enlight.anima.animamagiccards.async.SpellsLoader;
import fr.enlight.anima.animamagiccards.databinding.FragmentSpellsStackBinding;
import fr.enlight.anima.animamagiccards.ui.spells.bo.SpellGradeLevel;
import fr.enlight.anima.animamagiccards.ui.spells.stackstategy.AllSpellsSpellStackStrategy;
import fr.enlight.anima.animamagiccards.ui.spells.stackstategy.FreeAccessSpellStackStrategy;
import fr.enlight.anima.animamagiccards.ui.spells.stackstategy.SpellStackStrategy;
import fr.enlight.anima.animamagiccards.ui.spells.stackstategy.SpellbookSpellStackStrategy;
import fr.enlight.anima.animamagiccards.ui.spells.stackstategy.WitchspellsSpellStackStrategy;
import fr.enlight.anima.animamagiccards.ui.spells.viewmodels.SpellFilterViewModel;
import fr.enlight.anima.animamagiccards.ui.spells.viewmodels.SpellStackViewModel;
import fr.enlight.anima.animamagiccards.ui.spells.viewmodels.SpellViewModel;
import fr.enlight.anima.animamagiccards.ui.spells.viewmodels.quickaccess.AbstractGroupQAViewModel;
import fr.enlight.anima.animamagiccards.utils.DeviceUtils;
import fr.enlight.anima.animamagiccards.utils.IntentsUtils;
import fr.enlight.anima.animamagiccards.utils.OnBackPressedListener;
import fr.enlight.anima.animamagiccards.views.bindingrecyclerview.BindableViewModel;
import fr.enlight.anima.cardmodel.business.SpellFilterFactory;
import fr.enlight.anima.cardmodel.business.SpellFilterManager;
import fr.enlight.anima.cardmodel.model.spells.Spell;
import fr.enlight.anima.cardmodel.model.spells.SpellActionType;
import fr.enlight.anima.cardmodel.model.spells.SpellGrade;
import fr.enlight.anima.cardmodel.model.spells.SpellType;
import fr.enlight.anima.cardmodel.model.spells.Spellbook;
import fr.enlight.anima.cardmodel.model.spells.SpellbookType;
import fr.enlight.anima.cardmodel.model.witchspells.Witchspells;


@SuppressWarnings("ConstantConditions")
public class SpellStackFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Spell>>,
        SpellViewModel.Listener,
        SearchView.OnQueryTextListener,
        AbstractGroupQAViewModel.Listener,
        CardStackView.ItemExpendListener,
        CustomCardStackView.ItemSelectionListener,
        OnBackPressedListener {

    private static final String EFFECT_DIALOG = "EFFECT_DIALOG";
    private static final String GRADE_DIALOG = "GRADE_DIALOG";

    private static final String SPELLBOOK_PARAM = "SPELLBOOK_PARAM";
    private static final String WITCHSPELLS_PARAM = "WITCHSPELLS_PARAM";
    private static final String FREE_ACCESS_LIMIT_PARAM = "FREE_ACCESS_LIMIT_PARAM";
    private static final String SHOW_ALL_SPELLS_PARAM = "SHOW_ALL_SPELLS_PARAM";

    private FragmentSpellsStackBinding binding;

    private SpellStackViewModel spellViewModels;
    private SpellFilterViewModel filterViewModel;
    @Nullable
    private AbstractGroupQAViewModel quickAccessViewModel;

    private SpellStackStrategy spellStackStrategy;

    private final List<SpellFilterFactory.SpellFilter> filters = new ArrayList<>();
    private SpellFilterFactory.SpellFilter mQuickAccessFilter;

    private boolean spellExpanded;
    private Spell mLastSelectedSpell;


    private CustomCardStackView mCardStack;

    private SearchView mSearchView;
    private MenuItem searchMenuItem;

    private Handler handler = new Handler();

    private Listener mListener;



    public static SpellStackFragment newInstanceForSpellbook(Spellbook spellbook) {
        SpellStackFragment fragment = new SpellStackFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(SPELLBOOK_PARAM, spellbook);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static SpellStackFragment newInstanceForWitchspells(Witchspells witchspells) {
        SpellStackFragment fragment = new SpellStackFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(WITCHSPELLS_PARAM, witchspells);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static SpellStackFragment newInstanceForFreeSpellSelection(int freeAccessLimit) {
        SpellStackFragment fragment = new SpellStackFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(FREE_ACCESS_LIMIT_PARAM, freeAccessLimit);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static SpellStackFragment newInstanceForSpellSelection() {
        Bundle args = new Bundle();
        args.putBoolean(SHOW_ALL_SPELLS_PARAM, true);
        SpellStackFragment fragment = new SpellStackFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_spells_stack, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCardStack = (CustomCardStackView) view.findViewById(R.id.card_stack_view);
        mCardStack.setAnimationType(CardStackView.ALL_DOWN);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create the spell stack strategy in function of fragment's arguments
        spellStackStrategy = createSpellStackStrategy(getArguments());

        // Toolbar
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(spellStackStrategy.getStackTitle(getActivity()));
        }
        setHasOptionsMenu(true);

        // Creating ViewModels
        spellViewModels = new SpellStackViewModel(this, this, spellStackStrategy.getCardBackground(), spellStackStrategy.getBackgroundTextColor());
        binding.setModel(spellViewModels);

        filterViewModel = new SpellFilterViewModel();
        binding.setFilterModel(filterViewModel);

        quickAccessViewModel = spellStackStrategy.createQuickAccessViewModel(this);
        if(quickAccessViewModel != null) {
            quickAccessViewModel.setLayoutManager(new LinearLayoutManager(getActivity()));
            mQuickAccessFilter = quickAccessViewModel.getCurrentQuickAccessFilter();
            binding.setQuickAccessModel(quickAccessViewModel);
        }

        // Then first load of filters
        reloadSpellFilters();
    }

    private SpellStackStrategy createSpellStackStrategy(Bundle arguments) {
        if (arguments.containsKey(WITCHSPELLS_PARAM)) {
            Witchspells witchspells = arguments.getParcelable(WITCHSPELLS_PARAM);
            return new WitchspellsSpellStackStrategy(witchspells);

        } else if (arguments.containsKey(SPELLBOOK_PARAM)) {
            Spellbook spellbook = arguments.getParcelable(SPELLBOOK_PARAM);
            return new SpellbookSpellStackStrategy(spellbook);

        } else if (arguments.containsKey(FREE_ACCESS_LIMIT_PARAM)) {
            int levelLimit = arguments.getInt(FREE_ACCESS_LIMIT_PARAM);
            return new FreeAccessSpellStackStrategy(levelLimit, true);

        } else if(arguments.containsKey(SHOW_ALL_SPELLS_PARAM)){
            return new AllSpellsSpellStackStrategy();
        }
        throw new IllegalStateException("Should have at least one parameter handled in arguments. This fragment must be created with one of the newInstance method.");
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            mListener = (Listener) context;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) {
            mListener = (Listener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();

        if (spellStackStrategy.isSelectionMode() && spellExpanded) {
            inflater.inflate(R.menu.validate_selection_menu, menu);

        } else {
            inflater.inflate(R.menu.search_menu, menu);

            searchMenuItem = menu.findItem(R.id.action_search);
            mSearchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
            mSearchView.setSubmitButtonEnabled(true);
            mSearchView.setOnQueryTextListener(this);

            MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    filterViewModel.filterPanelVisible.set(true);
                    getActivity().invalidateOptionsMenu();

                    // Trick to make the keyboard not showing on search view expand
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            DeviceUtils.hideSoftKeyboard(getActivity().getCurrentFocus());
                            CharSequence searchQuery = filterViewModel.getSearchQuery();
                            if (searchQuery != null) {
                                mSearchView.setQuery(searchQuery, false);
                            }
                        }
                    }, 10);

                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    filterViewModel.filterPanelVisible.set(false);
                    getActivity().invalidateOptionsMenu();
                    return true;
                }
            });

            if (filterViewModel.filterPanelVisible.get()) {
                inflater.inflate(R.menu.validate_filter_menu, menu);
            } else {
                inflater.inflate(R.menu.notify_error_menu, menu);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            } else {
                getActivity().onBackPressed();
            }
            return true;

        } else if (item.getItemId() == R.id.action_validate_filter) {
            filterViewModel.setSearchQuery(mSearchView.getQuery());
            reloadSpellFilters();
            searchMenuItem.collapseActionView();
            return true;

        } else if (item.getItemId() == R.id.action_validate_spell) {
            if (mLastSelectedSpell == null) {
                throw new IllegalStateException("Last selected spell should not be null at this point");
            }
            mListener.onSpellSelected(mLastSelectedSpell);
            return true;

        } else if (item.getItemId() == R.id.action_notify_error){
            if(mLastSelectedSpell != null){
                startActivity(IntentsUtils.navigateToSendMisspellingMail(getActivity(), mLastSelectedSpell));
            } else {
                startActivity(IntentsUtils.navigateToSendMisspellingMail(getActivity()));
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // //////////////
    // region Filters
    // //////////////

    @Override
    public boolean onQueryTextSubmit(String query) {
        filterViewModel.setSearchQuery(query);
        reloadSpellFilters();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }


    private void reloadSpellFilters() {
        filters.clear();

        SpellFilterFactory spellFilterFactory = new SpellFilterFactory();

        CharSequence searchQuery = filterViewModel.getSearchQuery();
        if (!TextUtils.isEmpty(searchQuery)) {
            filters.add(spellFilterFactory.createSearchSpellFilter(searchQuery.toString(), filterViewModel.isSearchWitDesc()));
        }

        List<SpellType> spellTypeList = filterViewModel.getSelectedSpellTypes();
        if (spellTypeList != null && !spellTypeList.isEmpty()) {
            filters.add(spellFilterFactory.createTypeSpellFilter(spellTypeList));
        }

        int intelligenceMaxValue = filterViewModel.getIntelligenceMaxValue();
        if (intelligenceMaxValue > 0) {
            filters.add(spellFilterFactory.createIntelligenceSpellFilter(intelligenceMaxValue));
        }

        int zeonMaxValue = filterViewModel.getZeonMaxValue();
        int retentionMaxValue = filterViewModel.getRetentionMaxValue();
        if (zeonMaxValue > 0 || retentionMaxValue > 0) {
            filters.add(spellFilterFactory.createZeonSpellFilter(zeonMaxValue, retentionMaxValue, filterViewModel.isDailyRetentionOnly()));
        }

        SpellActionType spellActionType = filterViewModel.getSpellActionType();
        if (spellActionType != null) {
            filters.add(spellFilterFactory.createActionTypeSpellFilter(spellActionType));
        }

        getLoaderManager().restartLoader(1, getArguments(), this);
    }

    // endregion

    @Override
    public Loader<List<Spell>> onCreateLoader(int id, Bundle args) {
        spellViewModels.stackVisible.set(false);
        SpellFilterManager spellFilterManager = new SpellFilterManager(filters, mQuickAccessFilter);

        return new SpellsLoader(getActivity(), spellStackStrategy, spellFilterManager);
    }

    @Override
    public void onLoadFinished(Loader<List<Spell>> loader, List<Spell> data) {
        List<BindableViewModel> result = new ArrayList<>();
        for (Spell spell : data) {
            SpellViewModel spellViewModel = new SpellViewModel(spell, spell.spellbookType);
            spellViewModel.setListener(this);
            spellViewModel.setReduced(quickAccessViewModel != null);
            result.add(spellViewModel);
        }

        spellViewModels.setViewModels(result);
        spellViewModels.stackVisible.set(true);

        mCardStack.scrollTo(0, 0);
    }

    @Override
    public void onLoaderReset(Loader<List<Spell>> loader) {
        spellViewModels.setViewModels(null);
    }

    @Override
    public void onItemExpend(boolean expend) {
        spellExpanded = expend;

        if (spellStackStrategy.isSelectionMode()) {
            spellStackStrategy.setSpellExpanded(spellExpanded);

            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            actionBar.setTitle(spellStackStrategy.getStackTitle(getActivity()));

            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onQuickAccessSelected(SpellFilterFactory.SpellFilter quickAccessFilter) {
        this.mQuickAccessFilter = quickAccessFilter;
        getLoaderManager().restartLoader(1, getArguments(), this);
    }

    @Override
    public void onEffectClicked(Spell spell, SpellbookType spellbookType) {
        SpellEffectDialogFragment.newInstance(spell, spellbookType)
                .show(getFragmentManager(), EFFECT_DIALOG);
    }

    @Override
    public void onGradeClicked(SpellGradeLevel level, SpellGrade spellGrade, Spell spell) {
        SpellGradeDialogFragment.newInstance(level, spellGrade, spell)
                .show(getFragmentManager(), GRADE_DIALOG);
    }

    @Override
    public void onItemSelected(int position) {
        if (position >= 0) {
            List<? extends BindableViewModel> viewModels = spellViewModels.getViewModels();
            if (viewModels != null && !viewModels.isEmpty()) {
                SpellViewModel spellViewModel = (SpellViewModel) viewModels.get(position);
                mLastSelectedSpell = spellViewModel.getSpell();
            }

        } else {
            mLastSelectedSpell = null;
        }
    }

    @Override
    public boolean onBackPressed() {
        if(mLastSelectedSpell != null && spellExpanded){
            mCardStack.unselectCard();
            return true;
        }
        return false;
    }



    public interface Listener {
        void onSpellSelected(Spell mLastSelectedSpell);
    }
}
