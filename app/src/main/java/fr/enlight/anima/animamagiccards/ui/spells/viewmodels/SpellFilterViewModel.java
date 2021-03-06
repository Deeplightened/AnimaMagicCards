package fr.enlight.anima.animamagiccards.ui.spells.viewmodels;


import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableBoolean;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.enlight.anima.animamagiccards.R;
import fr.enlight.anima.cardmodel.model.spells.SpellActionType;
import fr.enlight.anima.cardmodel.model.spells.SpellType;

public class SpellFilterViewModel extends ViewModel {

    public final ObservableBoolean filterPanelVisible = new ObservableBoolean(false);

    private CharSequence searchQuery;
    private boolean searchWitDesc;
    private final Set<SpellType> selectedSpellTypes = new HashSet<>();
    private String intelligenceMax;
    private String zeonMax;
    private String retentionMax;
    private boolean dailyRetentionOnly;
    private int spellActionTypeSelectedId;

    public CharSequence getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(CharSequence searchQuery) {
        this.searchQuery = searchQuery;
    }

    public boolean isSearchWitDesc() {
        return searchWitDesc;
    }

    public void setSearchWitDesc(boolean searchWitDesc) {
        this.searchWitDesc = searchWitDesc;
    }

    public String getIntelligenceMax() {
        return intelligenceMax;
    }

    public void setIntelligenceMax(String intelligenceMax) {
        this.intelligenceMax = intelligenceMax;
    }

    public boolean isSpellTypeChecked(SpellType spellType){
        return selectedSpellTypes.contains(spellType);
    }

    public void onSpellTypeChecked(boolean checked, SpellType spellType){
        if(checked){
            selectedSpellTypes.add(spellType);
        } else {
            selectedSpellTypes.remove(spellType);
        }
    }

    public String getZeonMax() {
        return zeonMax;
    }

    public void setZeonMax(String zeonMax) {
        this.zeonMax = zeonMax;
    }

    public String getRetentionMax() {
        return retentionMax;
    }

    public void setRetentionMax(String retentionMax) {
        this.retentionMax = retentionMax;
    }

    public boolean isDailyRetentionOnly() {
        return dailyRetentionOnly;
    }

    public void setDailyRetentionOnly(boolean dailyRetentionOnly) {
        this.dailyRetentionOnly = dailyRetentionOnly;
    }

    public int getSpellActionTypeSelectedId() {
        return spellActionTypeSelectedId;
    }

    public void setSpellActionTypeSelectedId(int spellActionTypeSelectedId) {
        this.spellActionTypeSelectedId = spellActionTypeSelectedId;
    }

    public List<SpellType> getSelectedSpellTypes(){
        return new ArrayList<>(selectedSpellTypes);
    }

    public SpellActionType getSpellActionType(){
        switch (spellActionTypeSelectedId){
            case R.id.spell_action_type_active:
                return SpellActionType.ACTIVE;
            case R.id.spell_action_type_passive:
                return SpellActionType.PASSIVE;
            default:
                return null;
        }
    }

    public int getIntelligenceMaxValue(){
        if(TextUtils.isEmpty(intelligenceMax)){
            return -1;
        }
        return Integer.parseInt(intelligenceMax);
    }

    public int getRetentionMaxValue(){
        if(TextUtils.isEmpty(retentionMax)){
            return -1;
        }
        return Integer.parseInt(retentionMax);
    }

    public int getZeonMaxValue(){
        if(TextUtils.isEmpty(zeonMax)){
            return -1;
        }
        return Integer.parseInt(zeonMax);
    }
}
