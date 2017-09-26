package fr.enlight.anima.animamagiccards.ui.spells.stackstategy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fr.enlight.anima.animamagiccards.MainApplication;
import fr.enlight.anima.animamagiccards.R;
import fr.enlight.anima.animamagiccards.ui.spells.viewmodels.quickaccess.AbstractGroupQAViewModel;
import fr.enlight.anima.animamagiccards.ui.spells.viewmodels.quickaccess.GroupQASpellbookPathViewModel;
import fr.enlight.anima.cardmodel.business.SpellBusinessService;
import fr.enlight.anima.cardmodel.business.SpellFilterManager;
import fr.enlight.anima.cardmodel.model.spells.Spell;
import fr.enlight.anima.cardmodel.model.spells.SpellbookType;
import fr.enlight.anima.cardmodel.model.witchspells.Witchspells;
import fr.enlight.anima.cardmodel.model.witchspells.WitchspellsPath;


public class WitchspellsSpellStackStrategy implements SpellStackStrategy {

    private final Witchspells witchspells;

    public WitchspellsSpellStackStrategy(Witchspells witchspells) {
        this.witchspells = witchspells;
    }

    @Override
    public String getStackTitle(Context context) {
        return context.getString(R.string.Witchspells_Name_Format, witchspells.witchName);
    }

    @Override
    public AbstractGroupQAViewModel createQuickAccessViewModel(AbstractGroupQAViewModel.Listener listener) {
        List<SpellbookType> spellbookTypes = new ArrayList<>();
        for (WitchspellsPath path : witchspells.witchPaths) {
            spellbookTypes.add(SpellbookType.getTypeFromBookId(path.pathBookId));
            if (path.secondaryPathBookId >= 0) {
                SpellbookType typeFromBookId = SpellbookType.getTypeFromBookId(path.secondaryPathBookId);
                if(typeFromBookId != null && !spellbookTypes.contains(typeFromBookId)) {
                    spellbookTypes.add(typeFromBookId);
                }
            }
        }
        for (Integer spellbookId : witchspells.chosenSpells.keySet()) {
            SpellbookType spellbookType = SpellbookType.getTypeFromBookId(spellbookId);
            if (spellbookType != null && !spellbookTypes.contains(spellbookType)) {
                spellbookTypes.add(spellbookType);
            }
        }

        if (!spellbookTypes.isEmpty()) {
            Collections.sort(spellbookTypes);
        }

        return new GroupQASpellbookPathViewModel(spellbookTypes, listener, true);
    }

    @Override
    public @DrawableRes
    int getCardBackground() {
        return R.drawable.witchspells_book_background;
    }

    @Override
    public int getBackgroundTextColor() {
        return android.R.color.white;
    }

    @Override
    public boolean isSelectionMode() {
        return false;
    }

    @SuppressLint("UseSparseArrays")
    @Override
    public List<Spell> loadSpells(SpellBusinessService spellBusinessService, SpellFilterManager spellFilterManager) {
        Set<Spell> spellsSet = new LinkedHashSet<>();
        String mDefSystemLanguage = MainApplication.mDefSystemLanguage;

        // Chosen spells
        Map<Integer, List<Spell>> chosenSpells;
        if (witchspells.chosenSpellsInstantiated != null) {
            chosenSpells = new HashMap<>(witchspells.chosenSpellsInstantiated);
        } else {
            chosenSpells = new HashMap<>();
        }

        for (WitchspellsPath witchPath : witchspells.witchPaths) {
            List<Spell> pathSpells = spellBusinessService.getBookFromIdWithType(witchPath.pathBookId, witchPath.pathLevel, mDefSystemLanguage);

            if (witchPath.secondaryPathBookId > 0) {
                pathSpells.addAll(spellBusinessService.getBookFromIdWithType(witchPath.secondaryPathBookId, witchPath.pathLevel, mDefSystemLanguage));
            }

            // Free access spells
            Map<Integer, Integer> freeAccessSpellsIds = witchPath.freeAccessSpellsIds;
            if (freeAccessSpellsIds != null && !freeAccessSpellsIds.isEmpty()) {
                List<Spell> freeAccessSpells = spellBusinessService.getBookFromIdWithType(SpellbookType.FREE_ACCESS.bookId, 100, mDefSystemLanguage);
                for (Integer spellPosition : freeAccessSpellsIds.keySet()) {
                    int spellId = freeAccessSpellsIds.get(spellPosition);
                    for (Spell freeAccessSpell : freeAccessSpells) {
                        if (freeAccessSpell.spellId == spellId) {
                            freeAccessSpell.level = spellPosition;
                            freeAccessSpell.freeAccessAssociatedType = pathSpells.get(0).spellbookType;
                            pathSpells.add(freeAccessSpell);
                            break;
                        }
                    }
                }
            }

            // Search if this path exists in chosen spells
            List<Spell> spells = chosenSpells.get(witchPath.pathBookId);
            if (spells != null && !spells.isEmpty()) {
                for (Spell spell : spells) {
                    if (!pathSpells.contains(spell)) {
                        pathSpells.add(spell);
                    }
                }

            }
            // IMPORTANT : at last, we delete the entry from the map, because we will create spells for each
            // entry left
            chosenSpells.remove(witchPath.pathBookId);

            Collections.sort(pathSpells);
            spellsSet.addAll(pathSpells);
        }

        if (!chosenSpells.isEmpty()) {
            for (Integer spellbookId : chosenSpells.keySet()) {
                List<Spell> spells = chosenSpells.get(spellbookId);
                Collections.sort(spells);
                spellsSet.addAll(spells);
            }
        }

        List<Spell> result = new ArrayList<>(spellsSet);

        return spellFilterManager.filterSpells(result);
    }

    @Override
    public void setSpellExpanded(boolean spellExpanded) {
        // Nothing to do here
    }
}